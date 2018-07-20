
package com.linbin.performance.tools.memery;

import android.os.SystemProperties;

import com.yealink.android.utils.YLog;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class HpFileCreator {

    private static final String TAG = HpFileCreator.class.getSimpleName();

    /** 内存上限 单位:b */
    private long mMemoryLimit = 100 * 1024 * 1024;
    /** hp文件根目录 */
    private static final String ROOT_PATH = "/data/system/dropbox/dump_hp/";
    /** hp文件名 */
    private String mPackageName = "";

    /** 检测延时 */
    private long mTestDelay = 1000;
    /** 检测间隔 */
    private long mTestPeriod = 1000 * 60;

    /** 内存检测定时器 */
    Timer mTimer = new Timer();

    public HpFileCreator(String packageName) {
        mPackageName = packageName;
    }
    
    public HpFileCreator(String packageName, long memoryLimit, long testPeriod){
        mPackageName = packageName;
        mTestPeriod = testPeriod;
        mMemoryLimit = memoryLimit;
    }

    /**
     * 创建值守测试线程
     */
    public void creatTestTimerTask() {
        File file = new File(ROOT_PATH + mPackageName);
        if (file.exists() && file.listFiles().length == 0) {
            mTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    testMemory();
                }
            }, mTestDelay, mTestPeriod);
        }
    }

    /**
     * 测试内存
     */
    public synchronized void testMemory() {
        try {
            long totalMemory = Runtime.getRuntime().totalMemory();
            if (totalMemory > mMemoryLimit) {
                File file = new File(ROOT_PATH + mPackageName);
                // 如果文件夹不存在，则不执行
                if (!file.exists() || file.listFiles().length != 0) {
                    mTimer.cancel();
                    return;
                }

                SystemProperties.set("com.yealink.debug.stop_collie","true");
                
                // 生成hp文件路径
                String hpRootPath = file.getAbsolutePath();
                if (!hpRootPath.endsWith("/")) {
                    hpRootPath += "/";
                }
                
                createDumpHpFile(hpRootPath,mPackageName);
            }
        } catch (Exception e) {
        }
    }

    /**
     * 生成hprof文件 <br>
     * <b>该方法可能耗时较长</b>
     * 
     * @param packageName 包名
     */
    public void createDumpHpFile(String hpRootPath, String packageName) {

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ssss");
        String createTime = sdf.format(new Date(System.currentTimeMillis()));
        String hprofPath = hpRootPath + packageName + "_" + createTime + ".hprof";

        // 生成hp文件
        try {
            android.os.Debug.dumpHprofData(hprofPath);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void setTestDelay(long testDelay) {
        mTestDelay = testDelay;
    }

    public void setTestPeriod(long testPeriod) {
        mTestPeriod = testPeriod;
    }

    /**
     * 单位:b
     * */
    public void setMemoryLimit(long memoryLimit) {
        mMemoryLimit = memoryLimit;
    }

}
