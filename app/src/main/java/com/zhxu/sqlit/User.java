package com.zhxu.sqlit;


import com.zhxu.sqlit.library.db.annotion.DbFiled;
import com.zhxu.sqlit.library.db.annotion.DbTable;

/**
 * <p>Description:
 *
 * @author xzhang
 */

@DbTable("tb_common_user")
public class User {

    @DbFiled("tb_name")
    public String name ;

    @DbFiled("tb_password")
    public String password ;

    @DbFiled("tb_age")
    public String age ;

}
