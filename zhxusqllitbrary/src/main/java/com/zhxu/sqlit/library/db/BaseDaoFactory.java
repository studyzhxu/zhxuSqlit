package com.zhxu.sqlit.library.db;

import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;

/**
 * <p>Description:Dao工厂
 *
 * @author xzhang
 */

public class BaseDaoFactory {

    /** 数据库路径 */
    private String sqliteDatabasePath ;

    /** 操作数据库 */
    private SQLiteDatabase sqLiteDatabase ;


    private static BaseDaoFactory instance = null ;

    public static BaseDaoFactory getInstance(){
        if(instance == null){
            synchronized (BaseDaoFactory.class){
                instance = new BaseDaoFactory() ;
            }
        }
        return instance ;
    }


    private BaseDaoFactory(){
        //获取路径
        sqliteDatabasePath = Environment.getExternalStorageDirectory().getAbsolutePath()+"/user.db" ;
        //打开数据库
        openDatabase();
    }

    /**
     * 获取DataHelper
     * @param clazz         BaseDao的子类字节码
     * @param entityClass   要存入对象的字节码
     * @param <T>
     * @param <M>
     * @return
     */
    public synchronized <T extends BaseDao<M>,M> T getDataHelper(Class<T> clazz,Class<M> entityClass){
        T dao = null ;
        //获取对象
        try {
            dao = clazz.newInstance() ;
            dao.init(entityClass,sqLiteDatabase) ;

        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        return dao;
    }


    /**
     * 打开或创建数据库
     */
    private void openDatabase() {
        this.sqLiteDatabase = SQLiteDatabase.openOrCreateDatabase(sqliteDatabasePath,null) ;
    }

}

