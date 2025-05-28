package me.monarch.tools;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class ClassFileConverter {

    public static List<ClassFile> convert(List<ScFile> scFiles) throws IOException {
        List<ClassFile> classFiles = new ArrayList<>();

        for (ScFile scFile : scFiles) {
            classFiles.add(convert(scFile));
        }

        return classFiles;
    }

    public static ClassFile convert(ScFile scFile) throws IOException {
        ClassFile classFile = new ClassFile();
        classFile.simpleName = scFile.simpleName.replace(".sc", "");
        classFile.fullName = scFile.fullName.replace("/", ".").replace(".sc", "");

        byte[] bytes;
        int magicNumber;
        short minorVersion;
        short majorVersion;

        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(scFile.content);
             DataInputStream dataInputStream = new DataInputStream(byteArrayInputStream)) {
            var mci = dataInputStream.readUTF();
            if (!"gamebox".equals(mci)) {
                return null;
            }

            magicNumber = dataInputStream.readInt(); // class file magic number 0xCAFEBABE
            dataInputStream.readByte(); // unknown
            minorVersion = dataInputStream.readShort(); // minor version
            majorVersion = dataInputStream.readShort(); // major version
            dataInputStream.readLong(); // timestamp

            // begin from constant_pool_count segment
            bytes = dataInputStream.readAllBytes();
        }

        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream)) {
            dataOutputStream.writeInt(magicNumber);
            dataOutputStream.writeShort(minorVersion);
            dataOutputStream.writeShort(majorVersion);
            dataOutputStream.write(bytes);

            classFile.data = byteArrayOutputStream.toByteArray();
        }

        return classFile;
    }
}
