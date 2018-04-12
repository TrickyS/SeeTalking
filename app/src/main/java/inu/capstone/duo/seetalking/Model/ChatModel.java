package inu.capstone.duo.seetalking.Model;

import com.google.firebase.database.IgnoreExtraProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by sdh15 on 2018-03-13.
 */
@IgnoreExtraProperties
public class ChatModel {

    public Map<String, Boolean> users = new HashMap<>(); // 채팅 유저
    public Map<String, Comment> comments = new HashMap<>(); // 채팅 내용

    public static class Comment {

        public String uid;
        public String message;
        public Object timestamp;
    }
}
