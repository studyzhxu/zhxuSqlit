package com.zhxu.sqlit.library.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import com.zhxu.sqlit.library.db.annotion.DbFiled;
import com.zhxu.sqlit.library.db.annotion.DbTable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <p>Description: BaseDao是真正与底层数据库打交道
 *
 * @author xzhang
 */

public class BaseDao<T> implements IBaseDao<T> {


    /** 持有数据库操作类的引用 */
    private SQLiteDatabase database ;

    /** 保证实例化一次 */
    private boolean isInit = false ;

    /** 持有操作数据库表所对应的Java类型 */
    private Class<T> entityClass ;

    /** 表名 */
    private String tableName ;

    /** 维护表名与成员变量的映射关系 */
    private HashMap<String,Field> cacheMap ;

    /** 初始化 */
    protected boolean init(Class<T> entity,SQLiteDatabase sqLiteDatabase){

        this.entityClass = entity ;
        if(!isInit){
            this.database = sqLiteDatabase ;
            //判断注解是否为null
            if(entity.getAnnotation(DbTable.class) == null){
                this.tableName = entity.getClass().getSimpleName() ;
            }else {
                this.tableName = entity.getAnnotation(DbTable.class).value();
            }

            //检查数据库是否打开
            if(!database.isOpen()){
                return false ;
            }

            //执行sql语句创建表
            if(!TextUtils.isEmpty(createTable())){
                database.execSQL(createTable());
            }
            initCacheMap();

            isInit = true ;

        }

        return isInit ;
    }

    /** 维护映射关系 */
    private void initCacheMap() {
        Cursor cursor = null ;

        try {
            /**
             * map集合中
             * key  列名
             * map  变量对象
             *
             * 主要功能是找到列名对应的变量对象，便于后续的使用等
             */
            cacheMap = new HashMap<>();
            //1 第一步需要查询一遍表获取列名
            String sql = "select * from " + this.tableName ;
            cursor = database.rawQuery(sql, null);

            //获取表的列名数组
            String[] columnNames = cursor.getColumnNames();
            //获取Field数组
            Field[] columnFields = entityClass.getFields();

            for (Field field : columnFields) {
                field.setAccessible(true);
            }

            //查找对应关系
            for (String colmunName : columnNames) {
                Field columField = null;
                for (Field field : columnFields) {
                    String fieldName = null;
                    //获取注解
                    DbFiled dbFiled = field.getAnnotation(DbFiled.class);
                    if (dbFiled != null) {
                        fieldName = dbFiled.value();
                    } else {
                        fieldName = field.getName();
                    }
                    //如果找到对应表的列名对应的成员变量
                    if (colmunName.equals(fieldName)) {
                        columField = field;
                        break;
                    }
                }
                //找到对应关系
                if (columField != null) {
                    cacheMap.put(colmunName, columField);
                }
            }
        }catch (Exception e){

        }finally {
            if(cursor != null)
                cursor.close() ;
        }

    }


    @Override
    public Long insert(T entity) {
        Map<String, String> map = getValues(entity);

        ContentValues values = getContentValues(map);
        long insert = database.insert(tableName, null, values);
        return insert;
    }

    /**
     * 将map转换成ContentValues
     * @param map
     * @return
     */
    private ContentValues getContentValues(Map<String, String> map) {
        ContentValues values = new ContentValues() ;
        for(String key : map.keySet()){
            values.put(key,map.get(key));
        }


        return values;
    }

    @Override
    public int update(T entity, T where) {
        int result = -1 ;
        //将修改的结果转换成Map集合
        Map<String, String> map = getValues(entity);
        //将修改的条件转换成Map集合
        Map<String, String> whereClause = getValues(where);
        //得到修改的条件语句
        Condition condition = new Condition(whereClause);

        ContentValues contentValues = getContentValues(map);
        result = database.update(tableName, contentValues, condition.getWhereClause(), condition.getWhereArgs());

        return result;
    }

    @Override
    public int delete(T where) {
        Map<String, String> map = getValues(where);
        Condition condition = new Condition(map) ;
        int result = database.delete(tableName, condition.getWhereClause(), condition.getWhereArgs());
        return result;
    }

    @Override
    public List<T> query(T where) {
        return query(where,null,null,null);
    }

    @Override
    public List<T> query(T where, String orderBy, Integer startIndex, Integer limit) {

        Map<String, String> map = getValues(where);
        String limitStr = null ;
        if(startIndex != null && limit != null){
            limitStr = startIndex + " , " + limit ;
        }

        Condition condition = new Condition(map) ;
        Cursor cursor = database.query(tableName, null, condition.getWhereClause(), condition.getWhereArgs(), null, null, orderBy, limitStr);
        List<T> result = getResult(cursor,where);

        return result;
    }
    /** 获取查询结果 */
    private List<T> getResult(Cursor cursor, T where) {
        List<T> list = new ArrayList<>() ;

        //定义变量用于接收查询到的数据
        T item ;
        while(cursor.moveToNext()){
            try {
                //通过反射初始化对象
                item = (T) where.getClass().newInstance();


                //下面循环对变量名进行赋值
                /**
                 * cacheMap中缓存的是
                 * key      列名
                 * value    成员变量名
                 *
                 */
                for(String key : cacheMap.keySet()) {
                    //得到数据库表中的列名
                    String columnName = key;
                    //然后通过列名获取游标的位置
                    int columnIndex = cursor.getColumnIndex(columnName);
                    //获取到对象中的成员变量名称
                    Field field = cacheMap.get(key);
                    //获取成员变量的类型
                    Class type = field.getType();

                    //反射方式给item中的变量赋值
                    if (columnIndex != -1){
                        if (type == String.class) {
                            field.set(item, cursor.getString(columnIndex));
                        }else if(type == Double.class){
                            field.set(item,cursor.getDouble(columnIndex));
                        }else if(type == Integer.class){
                            field.set(item,cursor.getInt(columnIndex));
                        }else if(type == byte[].class){
                            field.set(item,cursor.getBlob(columnIndex));
                        }else{
                            continue ;
                        }
                    }
                }
                //将变量存入到集合中
                list.add(item);
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        return list;
    }

    /** 将对象转换成map集合 */
    private Map<String,String> getValues(T entity){
        /**
         * 集合
         * key 列名也是变量上的注解值
         * value 变量的具体值
         */
        HashMap<String,String> result = new HashMap<>() ;
        Iterator<Field> fieldIterator = cacheMap.values().iterator();
        //循环遍历映射表  遍历cacheMap得到列名和其对应的变量对象（cacheMap中存入的是列名和对象的映射）
        while(fieldIterator.hasNext()){
            //得到成员变量
            Field colmunToField = fieldIterator.next();
            //定义变量用于存储变量上注解的值，也就是列名
            String cacheKey = null ;
            //定义变量用于存储变量的具体值
            String cacheValue = null ;

            //获取列名
            if(colmunToField.getAnnotation(DbFiled.class) != null){
                cacheKey = colmunToField.getAnnotation(DbFiled.class).value();

            }else {
                cacheKey = colmunToField.getName();
            }

            try {
                if(colmunToField.get(entity) == null){
                    continue;
                }
                //得到具体的变量的值
                cacheValue = colmunToField.get(entity).toString();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }

            result.put(cacheKey,cacheValue) ;

        }

        return result ;
    }

    /**
     * 获取创建数据库表的sql
     * @return
     */
    private String createTable(){

        HashMap<String,String> columMap = new HashMap<>();

        Field[] fields = entityClass.getFields();
        for(Field field : fields){
            field.setAccessible(true);
            DbFiled dbFiled = field.getAnnotation(DbFiled.class);
            if(dbFiled == null){
                columMap.put(field.getName(),field.getName());
            }else {
                columMap.put(field.getName(),dbFiled.value());
            }
        }

        //创建数据库语句
        String sql = "create table if not exists "+ tableName + "(" ;
        Set<String> keys = columMap.keySet();
        StringBuilder sb = new StringBuilder() ;
        for(String key : keys){
            String value = columMap.get(key);
            sb.append(value).append(" varchar(20)").append(",");
        }
        String s = sb.toString();
        s = s.substring(0,s.lastIndexOf(",")) + ")" ;
        //拼接sql语句
        sql = sql + s ;
        return sql ;
    }

    /**
     * 封装修改的语句
     */
    class Condition {
        private String whereClause ;

        private String[] whereArgs ;

        public Condition(Map<String, String> whereClause) {
            ArrayList<String> list = new ArrayList<>() ;
            StringBuilder sb = new StringBuilder() ;

            sb.append("1=1") ;
            for(String key : whereClause.keySet()){
                String value = whereClause.get(key);
                if(value != null){
                    //拼接条件查询语句
                    sb.append(" and ").append(key).append(" =?");
                    //查询条件
                    list.add(value);
                }
            }
            this.whereClause = sb.toString() ;
            this.whereArgs = list.toArray(new String[list.size()]);
        }

        public String getWhereClause() {
            return whereClause;
        }

        public String[] getWhereArgs() {
            return whereArgs;
        }
    }


}
