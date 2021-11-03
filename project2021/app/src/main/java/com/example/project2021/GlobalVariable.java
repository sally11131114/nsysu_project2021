package com.example.project2021;

import android.app.Application;

import java.net.Socket;

public class GlobalVariable extends Application {
    public Socket test;
    private String Name;     //User 名稱
    private int Age;         //User 年紀

    //修改 變數値
    public void setName(String name){
        this.Name = name;
    }
    public void setAge(int age){
        this.Age = age;
    }

    //取得 變數值
    public String getName() {
        return Name;
    }
    public int getAge(){
        return Age;
    }
}

