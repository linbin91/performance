
package com.linbin.performance.tools.memery;

import android.os.Environment;
import android.text.TextUtils;


import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * SD卡相关的辅助类
 * 
 * @author zhy
 */
public class SDCardUtils {

    private final static String TAG = SDCardUtils.class.getSimpleName();

    private SDCardUtils() {
        /* cannot be instantiated */
        throw new UnsupportedOperationException("cannot be instantiated");
    }



    /**
     * 判断文件路径是否存在
     */
    public static boolean fileIsExists(String name) {
        try {
            File f = new File(name);
            if (!f.exists()) {
                return false;
            }

        } catch (Exception e) {
            // TODO: handle exception
            return false;
        }
        return true;
    }

    /**
     * 获取应用所占用的内存大小
     *
     * @param pkgName 应用的包名
     * @return 占用内存的大小（kB），包括native heap 和 dalvik heap等，为总内存大小
     */
    public static int getProcessMemory(String pkgName) {
        int memoryUsed = 0;
        String getMemory = "dumpsys meminfo | grep " + pkgName;
        ShellUtils.CommandResult getMemoryResult = ShellUtils.execCommand(
                getMemory, false);
        if (getMemoryResult.successMsg.trim().length() > 0) {
            String str = getMemoryResult.successMsg;
            int end = str.indexOf(" kB:");
            memoryUsed = Integer.parseInt(str.substring(0, end).trim());
        }
        return memoryUsed;
    }

    /**
     * dump应用的hprof文件,为了保证数据的完整性，此步骤将耗时1分钟 hprof文件存放在/sdcard/autotest/hprof路径下
     * 
     * @param pkgName 进程名称
     */
    public static void getHprof(final String pkgName) {

        Thread dumpThread = new Thread(new Runnable() {

            @Override
            public void run() {
                if(TextUtils.isEmpty(pkgName)){
                    return;
                }
                // 1、得到进程号
                String findPID = getPidByName(pkgName);
                if (findPID == null) {
                    return;
                }

                String LOG_PATH = "/dump.gc/";
               
                String state = android.os.Environment.getExternalStorageState();
                // 判断SdCard是否存在并且是可用的
                if (android.os.Environment.MEDIA_MOUNTED.equals(state)) {
                    File file = new File(
                            Environment.getExternalStorageDirectory().getPath() + LOG_PATH);
                    if (!file.exists()) {
                        return;
                    }
                    
                    if (file.listFiles().length!=0){
                        return;
                    }
                    
                    // 生成文件路径
                    String hprofPath = file.getAbsolutePath();
                    if (!hprofPath.endsWith("/")) {
                        hprofPath += "/";
                    }

                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ssss");
                    String createTime = sdf.format(new Date(System.currentTimeMillis()));
                    hprofPath += pkgName + "_" + createTime + ".hprof";
                    
                    // 2、根据进程号dumpheap
                    String dumpHeap = "am dumpheap " + findPID + " " + hprofPath;
                    ShellUtils.execCommand(dumpHeap, false);
                } else {
                }
            }
        });
        dumpThread.start();
    }

    /**
     * 通过包名查找pid，如果是system权限，该方法将失效，如果进程不在，则返回null
     *
     * @param pkgName 包名
     * @return pid
     */
    public static String getPidByName(String pkgName) {
        if(TextUtils.isEmpty(pkgName)){
            YLog.d("getHprof","getPidByName is null");
            return null;
        }
        String findPID = "ps | grep " + pkgName;
        ShellUtils.CommandResult result = ShellUtils
                .execCommand(findPID, false);
        if (result.successMsg.trim().length() > 0) {
            String[] strs = result.successMsg.split(" ");
            ArrayList<String> list = new ArrayList<String>();
            for (int i = 0; i < strs.length; i++) {
                if (strs[i].trim().length() > 0) {
                    list.add(strs[i]);
                }
            }
            return list.get(1);
        } else {
            return null;
        }

    }

}
