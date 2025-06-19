package com.financal.mgt.Financal.Management.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class FinalResponse {

    private String message;
    private int statusCode;
    private Object data;
    private String token;


    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    @Override
    public String toString() {
        return "FinalResponse{" +
                "message='" + message + '\'' +
                ", statusCode=" + statusCode +
                ", data=" + data +
                ", token='" + token + '\'' +
                '}';
    }
}
