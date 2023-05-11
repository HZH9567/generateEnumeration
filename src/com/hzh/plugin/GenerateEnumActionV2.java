package com.hzh.plugin;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GenerateEnumActionV2 extends AnAction {

    private static final String INPUT_REGEX = "(\\d+):\\s*(\\w+)";
    private static final Pattern INPUT_PATTERN = Pattern.compile(INPUT_REGEX);

    @Override
    public void actionPerformed(AnActionEvent e) {
        // 创建自定义的对话框
        MyDialog dialog = new MyDialog();
        dialog.show();

        // 如果用户点击 OK 按钮，解析用户输入的字符串，生成枚举类代码，并保存到指定目录下
        if (dialog.isOK()) {
            String input = dialog.inputField.getText();
            String enumName = dialog.enumNameField.getText();
            String result = generateEnumCode(input, enumName);

            // 保存文件到指定目录下
            VirtualFile projectDir = e.getProject().getBaseDir();
            VirtualFile genDir = projectDir.findChild("gen_code");
            if (genDir == null) {
                try {
                    genDir = projectDir.createChildDirectory(this, "gen_code");
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
            try {
                File file = new File(genDir.getPath(), enumName + ".java");
                FileWriter writer = new FileWriter(file);
                writer.write(result);
                writer.flush();
                writer.close();
                VirtualFile virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file);
                if (virtualFile != null) {
                    Messages.showInfoMessage("Generated enum file saved to " + file.getPath(), "Success");
                } else {
                    Messages.showErrorDialog("Failed to save generated enum file.", "Error");
                }
            } catch (IOException ex) {
                Messages.showErrorDialog("Failed to save generated enum file: " + ex.getMessage(), "Error");
            }
        }
    }

    // 解析用户输入的字符串，生成枚举类代码
    private String generateEnumCode(String input, String enumName) {
        String[] lines = input.split(",");
        StringBuilder builder = new StringBuilder();
        builder.append("public enum ").append(enumName).append(" {\n");
        for (String line : lines) {
            Matcher matcher = INPUT_PATTERN.matcher(line.trim());
            if (matcher.matches()) {
                int code = Integer.parseInt(matcher.group(1));
                String description = matcher.group(2);
                builder.append("    ")
                        .append(description.toUpperCase().replace(" ", "_"))
                        .append("(")
                        .append(code)
                        .append(", \"")
                        .append(description)
                        .append("\"),\n");
            } else {
                Messages.showErrorDialog("Invalid input: " + line, "Error");
                return null;
            }
        }
        builder.append(";\n\n");
        builder.append("    private final int code;\n");
        builder.append("    private final String description;\n\n");
        builder.append("    ").append(enumName).append("(int code, String description) {\n");
        builder.append("        this.code = code;\n");
        builder.append("        this.description = description;\n");
        builder.append("    }\n\n");
        builder.append("    public int getCode() {\n");
        builder.append("        return code;\n");
        builder.append("    }\n\n");
        builder.append("    public String getDescription() {\n");
        builder.append("        return description;\n");
        builder.append("    }\n");
        builder.append("}\n");
        return builder.toString();
    }

    // 自定义对话框类
    private static class MyDialog extends DialogWrapper {

        final JTextField enumNameField = new JTextField();
        final JTextArea inputField = new JTextArea(10, 30);

        public MyDialog() {
            super(true);
            init();
            setTitle("Generate Enum");
        }

        @Nullable
        @Override
        protected ValidationInfo doValidate() {
            if (enumNameField.getText().isEmpty()) {
                return new ValidationInfo("Enum name cannot be empty", enumNameField);
            }
            if (inputField.getText().isEmpty()) {
                return new ValidationInfo("Enum codes cannot be empty", inputField);
            }
            return null;
        }

        @Nullable
        @Override
        protected JComponent createCenterPanel() {
            JPanel panel = new JPanel(new BorderLayout(10, 10));
            panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            // 枚举类名称输入框
            JPanel enumNamePanel = new JPanel(new BorderLayout(10, 10));
            JLabel enumNameLabel = new JLabel("Enum name:");
            enumNamePanel.add(enumNameLabel, BorderLayout.WEST);
            enumNamePanel.add(enumNameField, BorderLayout.CENTER);
            panel.add(enumNamePanel, BorderLayout.NORTH);

            // 枚举常量信息输入框
            JPanel inputPanel = new JPanel(new BorderLayout(10, 10));
            JLabel inputLabel = new JLabel("Enum codes (e.g. 1: Waiting, 2: Processing, 3: Done):");
            inputPanel.add(inputLabel, BorderLayout.NORTH);
            inputPanel.add(new JScrollPane(inputField), BorderLayout.CENTER);
            panel.add(inputPanel, BorderLayout.CENTER);

            return panel;
        }

        @Override
        protected void doOKAction() {
            if (doValidate() == null) {
                super.doOKAction();
            }
        }
    }
}