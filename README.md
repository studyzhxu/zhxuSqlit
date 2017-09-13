# zhxuSqlit
自己动手撸一个数据库框架

#Android 手写数据库框架

##前言
在Android开发中，一定会遇到数据库sqlit的操作的，如果你的项目中没有用到数据库那么说明你的项目很失败。

一般我们可以直接使用系统提供的sqlit操作完成数据库的操作，同时也可以使用现在比较多的数据库开源框架，比如GreenDAO  OrmLitem等数据库框架，都是直接将对象映射到sqlit数据库的ORM框架。

在这篇文章中我们将自己动手写一个ORM框架，自定义一个属于我们自己的ORM数据库框架。

##原理分析
在Android中无论我们如何对数据库进行封装，最终操作都离不开sqlit自身对数据的增删改操作，所以我们需要将这些操作封装在底层，上层只需要传入对象调用相关方法即可，不用去管底层是如何做的，包括表的创建等。

好，下面我们来看看分析的图

![](http://img.my.csdn.net/uploads/201709/13/1505309223_4590.png)

从图中我们也可以看出来，手写数据库框架的主要内容就在中间部分，主要的有BaseDaoFactory和BaseDao这两个类。

但是在这些之前我们还有两个地方需要关注，就是数据库表的生成。在常用的数据库框架中如GreenDAO和ORMLitem等都是通过注解来生成表和字段的，那么在我们的框架中当然也采用这种方式来完成，下面就来看看代码吧

**特此声明，如果是Android studio用户，在使用该库时请关闭Instant Run功能，具体什么原因可以自己手动尝试**

##注解
生成表的注解

	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.RUNTIME)
	public @interface DbTable {
	    String value();
	}

生成字段的注解

	@Target(ElementType.FIELD)
	@Retention(RetentionPolicy.RUNTIME)
	public @interface DbFiled {
	    String value() ;
	}

这些注解该如何使用呢？

	@DbTable("tb_common_user")
	public class User {
	
	    @DbFiled("tb_name")
	    public String name ;
	
	    @DbFiled("tb_password")
	    public String password ;
	
	    @DbFiled("tb_age")
	    public String age ;
	    
	}

我们只需要在JavaBean类和变量上标注即可，这样就可以生成对应的表名和字段名，具体如何生成的，我们会在下面讲到，如果对注解知识不是特别了解，那就需要加强一下Java基础了哦。

既然知道了注解生成表和字段并且知道如何使用后，下面我们就来看看Dao层的代码吧

##BaseDaoFactory
具体的代码如下

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
	        //获取数据库路径
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

BaseDaoFactory代码内容不是太多，好，接下来我们就具体分析吧。

可以看出BaseDaoFactory采用单例的方式，用来生成Dao对象的。主要方法有两个openDatabase()和getDataHelper()方法，openDatabase()方法是负责获取sqliteDatabase对象的，因为sqlit底层操作需要这个对象。

getDataHelper()中只做了两件事，创建爱你Dao层对象，并且调用dao的init()方法。所以要想使用Dao我们只需要调用getDataHelper()方法传入我们想要使用的Dao，BaseDaoFactory会帮我们生成。

其中getDataHelper需要两个泛型参数，可能会让人有些费解，那我们就来看看这些泛型参数的含义

	public synchronized <T extends BaseDao<M>,M> T getDataHelper(Class<T> clazz,Class<M> entityClass){
		.....
	}

因为在这个框架中，所有的Dao层都一个基类，就是BaseDao，所以通过

> <T extends BaseDao

限定了T的类型，就是必须是BaseDao的基类，除了泛型T还有一个泛型M，M代表的是我们需要存入数据库的对象，比如上面讲到的User对象。

这里提到了BaseDao，那我们就来看看其中的具体方法吧，首先来看看init()方法，因为在BaseDaoFactory中调用了这个方法。

##BaseDao
首先看看IBaseDao代码

IBaseDao代码如下
	public interface IBaseDao<T> {
	    /**
	     * 插入一个对象到数据库
	     * @param entity 要插入的对象
	     * @return
	     */
	    public Long insert(T entity) ;
	
	    /**
	     * 更新
	     * @param entity
	     * @param where
	     * @return
	     */
	    public int update(T entity ,T where) ;
	
	    /**
	     * 删除
	     * @param where
	     * @return
	     */
	    public int delete(T where);
	
	    /**
	     * 查询
	     * @param where
	     * @return
	     */
	    public List<T> query(T where) ;
	
	    public List<T> query(T where,String orderBy,Integer startIndex,Integer limit) ;
	}

BaseDao代码如下

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
		........
	}

通过init()方法我们可以看出来，之前定义的注解这这里得到了使用，通过传入的对象获取注解和值，然后得到表名。这里还调用了两个方法，createTable和initCacheMap方法。

createTable是创建表的方法具体代码如下

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

通过代码我们也可以看出来在createTable()方法中我们通过获取变量上的注解获取到表中的列名然后拼接成sql语句，然后调用这个sql语句创建表。

还有一个initCacheMap()方法代码如下

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
在initCacheMap()方法中就做了一件事，将列名和对应的变量对象存入到map集合中，在之后会使用到。

下面我们就来看看具体的数据库操作方法吧。

##保存数据
首先insert方法代码如下

	@Override
    public Long insert(T entity) {
        Map<String, String> map = getValues(entity);

        ContentValues values = getContentValues(map);
        long insert = database.insert(tableName, null, values);
        return insert;
    }

通过代码我们可以看出来getValues()方法是将对象转换成Map集合，getContentValues()方法是将map集合转换成ContentValues，得到ContentValues对象后，我们就可以直接调用database.insert()方法插入数据了。

那我们来看看getValues()方法和getContentValues()方法吧

getValues()代码如下

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

具体getValues()是如何将对象转换成Map集合的这里就不再多说了，代码中注释写的比较清楚，就是通过获取注解和反射获取变量的具体值。

getContentValues()方法代码如下

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

这个方法就比较简单了，就是遍历map集合完成操作。

通过上面的分析基本上就可以理清楚思路了，也知道如何完成数据库表的创建和数据的保存了。接下来接看看数据的修改吧

##修改数据

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

修改代码中除了使用了前面讲到了getValues()方法和getContentVlaues()方法外还用到了Condition。

Condition代码如下


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

Condition是一个队修改语句的封装，类中通过拼接和转换获取到修改的条件语句和参数。

##删除数据

    @Override
    public int delete(T where) {
        Map<String, String> map = getValues(where);
        Condition condition = new Condition(map) ;
        int result = database.delete(tableName, condition.getWhereClause(), condition.getWhereArgs());
        return result;
    }

删除代码比较简单，也是调用了getValues()方法将条件对象转换成Map集合，然后通过Condition将集合装换成删除的条件语句和参数。

##查询数据

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

首先上面两个方法主要的是第二个，在代码中首先根据条件获取到了cursor对象，然后通过getResult()方法和cursor得到了最终对象集合

getResult代码如下

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

首先我们知道数据库中查询到的内容都在cursor中，所以我们只需要遍历cursor就可以获取到我们想要的内容，因为cursor中获取到的值需要赋值给对象，所以我们手动创建了T类型的对象，因为这个对象不确定，所以我们通过泛型表示。在之前的initCacheMap()方法中我们已经获取到了对象内部的变量名和表中的列名，所以可以通过反射获取到变量的类型，并对其进行赋值。

这样就完成了对变量的赋值了，最后将对象存入到list集合中然后返回。

OK完成

##使用
上面将框架的各个知识点讲完了还没有具体的使用呢，所以接下里我们就来使用我们手撸的框架

User类代码如下
	@DbTable("tb_common_user")
	public class User {
	
	    @DbFiled("tb_name")
	    public String name ;
	
	    @DbFiled("tb_password")
	    public String password ;
	
	    @DbFiled("tb_age")
	    public String age ;
	
	}

UserDao代码如下

	public class UserDao extends BaseDao<User> {
	}


MainActivity代码如下

	public class MainActivity extends AppCompatActivity {
	
	    @Override
	    protected void onCreate(Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);
	        setContentView(R.layout.activity_main);
	    }
	
	    //保存
	    public void save(View view){
	
	        Random random = new Random() ;
	
	        UserDao userDao = BaseDaoFactory.getInstance().getDataHelper(UserDao.class, User.class);
	        User user = new User();
	        user.name = "lilei" ;
	        user.password = "abc" ;
	        user.age = random.nextInt() % 2 == 0 ? "男" : "女" ;
	        userDao.insert(user);
	    }
	
	    //更新
	    public void update(View view){
	        UserDao userDao = BaseDaoFactory.getInstance().getDataHelper(UserDao.class, User.class);
	
	        //更新条件
	        User where = new User() ;
	        where.name = "lilei" ;
	
	        //更新为
	        User user = new User() ;
	        user.name = "hanmeimei" ;
	        userDao.update(user,where);
	    }
	
	    //删除
	    public void delete(View view){
	        UserDao userDao = BaseDaoFactory.getInstance().getDataHelper(UserDao.class, User.class);
	
	        //删除条件
	        User where = new User() ;
	        where.name = "hanmeimei";
	        userDao.delete(where);
	    }
	
	    //查询
	    public void query(View view){
	        UserDao userDao = BaseDaoFactory.getInstance().getDataHelper(UserDao.class, User.class);
	
	        User where = new User() ;
	        where.name = "lilei" ;
	        where.age = "女" ;
	        List<User> query = userDao.query(where);
	
	        for(User user : query){
	            System.out.println("name:"+user.name+",age:"+user.age+",password:"+user.password);
	        }
	    }
	}

好了结果我就不展示了，一遍通过。


##总结
通过上面的讲解，发现手写一个数据库其实也不是很难，当然这个框架有很多的不足的地方，但是至少让我们了解了如何手动撸一个自己的数据库框架，了解了数据库框架的原理。之后如果有什么想法当然可以在此基础上再添加。

最后代码地址[https://github.com/studyzhxu/zhxuSqlit](https://github.com/studyzhxu/zhxuSqlit)

QQ交流群

![](http://img.my.csdn.net/uploads/201709/08/1504853209_3603.png)

微信公众号：Android在路上，欢迎关注

![](http://img.my.csdn.net/uploads/201709/08/1504853083_7868.jpg)
