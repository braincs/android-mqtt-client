package com.braincs.attrsc.androidmqttclient.utils;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

/**
 * Created by Shuai
 * 14/03/2019.
 */

public class FileUtil {

    private final static String TAG = FileUtil.class.getSimpleName();
    private static File appFolder;


    public static String createFolderUnderAppFolder(String name){
        String directory = Environment.getExternalStorageDirectory().getAbsolutePath();
        File folder = new File(directory + File.separator + Constants.APP_NAME + File.separator + name);
        if (!folder.exists()){
            boolean isDone = folder.mkdirs();
            if (isDone){
                return folder.getAbsolutePath();
            }else {
                return null;
            }
        }
        return folder.getAbsolutePath();
    }
    public static boolean checkFolderUnderAppFolder(String name){
        String directory = Environment.getExternalStorageDirectory().getAbsolutePath();
        File folder = new File(directory + File.separator + Constants.APP_NAME + File.separator + name);
        return folder.exists();
    }
    public static String delFolderUnderAppFolder(String name){
        String directory = Environment.getExternalStorageDirectory().getAbsolutePath();
        File folder = new File(directory + File.separator + Constants.APP_NAME + File.separator + name);
        deleteRecursive(folder);
        return folder.getAbsolutePath();
    }

    private static void deleteRecursive(File folder){
        if (folder.isDirectory())
            for (File child : folder.listFiles())
                deleteRecursive(child);

        folder.delete();
    }

    public static List<String> getFoldersUnderAppFolder(){
        List<String> list = new LinkedList<>();
        String directory = Environment.getExternalStorageDirectory().getAbsolutePath();
        File folder = new File(directory + File.separator + Constants.APP_NAME );
        if (!folder.exists()){
            return list;
        }
        File[] files = folder.listFiles();
        if (files == null || files.length == 0) return list;
        for (File f :files) {
            if (f.isDirectory()){
                list.add(f.getName());
            }
        }
        return list;
    }

    public static String getFilePathUnderAppFolder(String fileName){
        if (appFolder == null || !appFolder.exists()){
            String directory = Environment.getExternalStorageDirectory().getAbsolutePath();
            appFolder = new File(directory + File.separator + Constants.APP_NAME);
            appFolder.mkdirs();
        }
        File file = new File(appFolder, fileName);
        if (!file.exists()){
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return file.getAbsolutePath();
    }
    public static File getFileUnderAppFolder(String fileName){
        if (appFolder == null || !appFolder.exists()){
            String directory = Environment.getExternalStorageDirectory().getAbsolutePath();
            appFolder = new File(directory + File.separator + Constants.APP_NAME);
            appFolder.mkdirs();
        }
        File file = new File(appFolder, fileName);
        if (!file.exists()){
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return file;
    }

    public static File createFileUnderGroup(String group, String face){
        if (appFolder == null || !appFolder.exists()){
            String directory = Environment.getExternalStorageDirectory().getAbsolutePath();
            appFolder = new File(directory + File.separator + Constants.APP_NAME);
            appFolder.mkdirs();
        }
        File parent = new File(appFolder.getAbsolutePath() + File.separator + group);
        if (!parent.exists()) return null;
        File file = new File(parent, face);
        if (!file.exists()){
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return file;
    }

    public static File getFileUnderGroup(String group, String face){
        if (appFolder == null || !appFolder.exists()){
            String directory = Environment.getExternalStorageDirectory().getAbsolutePath();
            appFolder = new File(directory + File.separator + Constants.APP_NAME);
            appFolder.mkdirs();
        }
        File parent = new File(appFolder.getAbsolutePath() + File.separator + group);
        if (!parent.exists()) return null;
        return new File(parent, face);
    }
    public static File getFileUnderFolder(File parentFile, String fileName){
        if (parentFile != null || !parentFile.exists()){
            parentFile.mkdirs();
        }
        File file = new File(parentFile, fileName);
        if (!file.exists()){
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return file;
    }
    public static int getFileCountAppFolder(){
        if (appFolder == null || !appFolder.exists()){
            String directory = Environment.getExternalStorageDirectory().getAbsolutePath();
            appFolder = new File(directory + File.separator + Constants.APP_NAME);
            appFolder.mkdirs();
        }
        int size = appFolder.listFiles().length;
        return size;
    }
    public static int getFileCountFolder(File file){
        if (file != null || !file.exists()){
            file.mkdirs();
        }
        int size = file.listFiles().length;
        return size;
    }
    /**
     * 合并多个pcm文件为一个wav文件
     *
     * @param filePathList    pcm文件路径集合
     * @param destinationPath 目标wav文件路径
     * @return true|false
     */
    public static boolean mergeFiles(List<String> filePathList,
                                     String destinationPath) {
        File[] file = new File[filePathList.size()];
        byte buffer[] = null;

        int TOTAL_SIZE = 0;
        int fileNum = filePathList.size();

        for (int i = 0; i < fileNum; i++) {
            file[i] = new File(filePathList.get(i));
            TOTAL_SIZE += file[i].length();
        }


        //先删除目标文件
        File destfile = new File(destinationPath);
        if (destfile.exists())
            destfile.delete();

        //合成所有的文件的数据，写到目标文件
        try {
            buffer = new byte[1024 * 4]; // Length of All Files, Total Size
            InputStream inStream = null;
            OutputStream ouStream = null;

            ouStream = new BufferedOutputStream(new FileOutputStream(
                    destinationPath));
            for (int j = 0; j < fileNum; j++) {
                inStream = new BufferedInputStream(new FileInputStream(file[j]));
                int size = inStream.read(buffer);
                while (size != -1) {
                    ouStream.write(buffer);
                    size = inStream.read(buffer);
                }
                inStream.close();
            }
            ouStream.close();
        } catch (FileNotFoundException e) {
            Log.e(TAG, e.getMessage());
            return false;
        } catch (IOException ioe) {
            Log.e(TAG, ioe.getMessage());
            return false;
        }
        clearFiles(filePathList);
        Log.i(TAG, "mergeFiles  success!" + new SimpleDateFormat("yyyy-MM-dd hh:mm").format(new Date()));
        return true;

    }

    /**
     * 清除文件
     *
     * @param filePathList
     */
    private static void clearFiles(List<String> filePathList) {
        for (int i = 0; i < filePathList.size(); i++) {
            File file = new File(filePathList.get(i));
            if (file.exists()) {
                file.delete();
            }
        }
    }
    /**
     * 将图片存入本地
     *
     * @param flag   保存的图片类型
     * @param data   图片数据
     * @param width  图片宽度
     * @param height 图片高度
     * @param isRed  是否是IR图片
     */
    public static String saveRawToFile(Context context, String flag, byte[] data, int width, int height, boolean isRed) {
        boolean mCanSaveRaw = 2048 * 1024 < getAvailMemory(context);
        if (!mCanSaveRaw || null == data) {
            return null;
        }

        String path = getFilePath(context,isRed ? "IR" + File.separator + flag : "RGB" + File.separator + flag);
        if (null == path) {
            return null;
        }

        File yuvDir = new File(path + File.separator + "yuv");
        if (!yuvDir.exists() && !yuvDir.mkdirs()) {
            return null;
        }

        File previewDir = new File(path);
        SimpleDateFormat mFileFormat = new SimpleDateFormat("yyyy-MM-dd_HH_mm_ss", Locale.CHINA);

        String fileName = "img_" + flag + "_" + mFileFormat.format(new Date());
        File rawFile = new File(yuvDir, fileName + ".yuv");
        FileOutputStream fos = null, bitFos = null;
        String ret = null;
        try {
            fos = new FileOutputStream(rawFile);
            fos.write(data);
            ret = rawFile.getAbsolutePath();

            // 额外保存一张可以预览的图片，方便客户直观查看。
//            Bitmap bitmap = ImageUtil.compress_nv21_to_argb(data, width, height);
//            if (null != bitmap) {
//                File previewFile = new File(previewDir, fileName + ".png");
//                bitFos = new FileOutputStream(previewFile);
//                bitmap.compress(Bitmap.CompressFormat.PNG, 100, bitFos);
//                bitFos.flush();
//                bitmap.recycle();
//                bitmap = null;
//            } else {
            File previewFile = new File(previewDir, fileName + ".jpg");
            bitFos = new FileOutputStream(previewFile);
            YuvImage yuvImage = new YuvImage(data, ImageFormat.NV21, width, height, null);
            yuvImage.compressToJpeg(new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight()), 100, bitFos);
            bitFos.flush();
//            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }

                if (null != bitFos) {
                    bitFos.close();
                }

                if (2048 * 1024 >= getAvailMemory(context)) {
                    Toast.makeText(context, "当前内存不足，无法继续保存图片", Toast.LENGTH_SHORT).show();
                    mCanSaveRaw = false;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return ret;
    }

    /**
     * 获取android当前可用内存大小
     *
     * @return
     */
    public static long getAvailMemory(Context mContext) {
        ActivityManager am = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
        if (null == am) {
            return 0;
        }

        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(mi);
        return mi.availMem;// 将获取的内存大小规格化
    }

    /**
     * 获取文件存储的路径
     *
     * @param flag
     * @return
     */
    public static String getFilePath(Context mContext, String flag) {
        String path = null;
        SimpleDateFormat mDirFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA);

        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            path = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator +
                    "Android" + File.separator + mContext.getPackageName() + File.separator +
                    mDirFormat.format(new Date()) + File.separator + flag;
        }
        if (null == path) {
            File externalCacheDir = mContext.getExternalCacheDir();
            if (null != externalCacheDir) {
                path = externalCacheDir.getAbsolutePath() + File.separator + mContext.getPackageName() +
                        File.separator + mDirFormat.format(new Date()) + File.separator + flag;
            }
        }
        if (null == path) {
            File cacheDir = mContext.getCacheDir();
            if (null != cacheDir)
                path = cacheDir.getAbsolutePath() + File.separator + mContext.getPackageName() +
                        File.separator + mDirFormat.format(new Date()) + File.separator + flag;
        }
        return path;
    }

}
