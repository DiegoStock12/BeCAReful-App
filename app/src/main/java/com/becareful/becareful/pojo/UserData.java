package com.becareful.becareful.pojo;

public class UserData {

    private String name;
    private String surname;

    public UserData(String name, String surname){
        this.name = name;
        this.surname = surname;
    }

    public String getName() {
        return name;
    }

    public String getSurname() {
        return surname;
    }
}
