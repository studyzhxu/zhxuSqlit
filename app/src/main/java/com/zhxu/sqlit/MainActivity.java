package com.zhxu.sqlit;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.zhxu.sqlit.library.db.BaseDaoFactory;

import java.util.List;
import java.util.Random;

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
