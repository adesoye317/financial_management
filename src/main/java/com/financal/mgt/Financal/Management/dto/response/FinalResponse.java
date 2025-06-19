package com.financal.mgt.Financal.Management.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FinalResponse {

    private String message;
    private int statusCode;
    private Object data;
    private String token;
}
