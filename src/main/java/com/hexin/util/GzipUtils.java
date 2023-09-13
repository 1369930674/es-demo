package com.hexin.util;

import org.apache.commons.lang3.StringUtils;
import sun.misc.BASE64Decoder;

import java.io.*;
import java.util.zip.GZIPInputStream;

public class GzipUtils {
    public static String decompressGzipText(String source) {
        if (StringUtils.isEmpty(source)) {
            return source;
        }
        ByteArrayOutputStream bos = null;
        ByteArrayInputStream bis = null;
        GZIPInputStream gis = null;
        byte[] compressBytes = null;
        String uncompressStr = null;
        try {
            bos = new ByteArrayOutputStream();
            compressBytes = new BASE64Decoder().decodeBuffer(source);
            bis = new ByteArrayInputStream(compressBytes);
            gis = new GZIPInputStream(bis);
            byte[] buffer = new byte[1024];
            int offset = -1;
            while (((offset = gis.read(buffer)) != -1)) {
                bos.write(buffer, 0, offset);
            }
            uncompressStr = bos.toString();
        } catch (Exception e) {

        } finally {
            if (null != gis) {
                try {
                    gis.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            if (null != bis) {
                try {
                    gis.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            if (null != bos) {
                try {
                    gis.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return uncompressStr;
    }
}