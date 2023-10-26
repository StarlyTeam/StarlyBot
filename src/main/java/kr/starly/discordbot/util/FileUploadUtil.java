package kr.starly.discordbot.util;

import net.dv8tion.jda.api.utils.FileUpload;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class FileUploadUtil {

    public static FileUpload createFileUpload(String text) {
        byte[] bytes = text.getBytes();
        InputStream inputStream = new BufferedInputStream(new ByteArrayInputStream(bytes));

        return FileUpload.fromData(inputStream, "list.txt");
    }
}