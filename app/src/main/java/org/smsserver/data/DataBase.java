package org.smsserver.data;

import static org.smsserver.config.Const.SEND_STATUS;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.smsserver.model.Message;

import java.util.ArrayList;
import java.util.Objects;

public class DataBase {

    private ArrayList<Message> dataBaseMessage = new ArrayList<>();

    public DataBase() {
    }

    public ArrayList<Message> getDataBaseMessage() {
        return dataBaseMessage;
    }

    public void uploadDataBase(String newMessages) {
        addElementInToBase(parsNewElement(newMessages));
    }

    public void addElementInToBase(ArrayList<Message> newMessageList) {
        dataBaseMessage.addAll(newMessageList);
    }

    private ArrayList<Message> parsNewElement(String newMessagesInJson) {
        ArrayList<Message> newList = new ArrayList<>();
        Gson gson = new Gson();
        try {
            newList = gson.fromJson(newMessagesInJson, new TypeToken<ArrayList<Message>>() {
            }.getType());
        } catch (Exception e) {

        }
        newList.forEach(message -> Log.d("MESSAGE", message.toString()));
        return newList;
    }

    public String getDatabaseToJson() {
        Gson gson = new Gson();
        return gson.toJson(dataBaseMessage.stream().
                filter(message -> Objects.equals(message.getStatus(), SEND_STATUS)));
    }
}
