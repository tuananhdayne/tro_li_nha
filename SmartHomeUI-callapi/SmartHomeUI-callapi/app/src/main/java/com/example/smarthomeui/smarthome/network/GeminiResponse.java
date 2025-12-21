package com.example.smarthomeui.smarthome.network;

import com.google.gson.annotations.SerializedName;

public class GeminiResponse {
    @SerializedName("question")
    private String question;
    
    @SerializedName("assistant")
    private GeminiAssistant assistant;
    
    public String getQuestion() {
        return question;
    }
    
    public GeminiAssistant getAssistant() {
        return assistant;
    }
    
    public static class GeminiAssistant {
        @SerializedName("action")
        private String action;
        
        @SerializedName("deviceType")
        private String deviceType;
        
        @SerializedName("command")
        private CommandData command;
        
        @SerializedName("message")
        private String message;
        
        public String getAction() {
            return action;
        }
        
        public String getDeviceType() {
            return deviceType;
        }
        
        public CommandData getCommand() {
            return command;
        }
        
        public String getMessage() {
            return message;
        }
        
        public boolean hasAction() {
            return action != null && !action.isEmpty();
        }
    }
    
    public static class CommandData {
        @SerializedName("method")
        private String method;
        
        @SerializedName("params")
        private Object params;
        
        public String getMethod() {
            return method;
        }
        
        public Object getParams() {
            return params;
        }
    }
}
