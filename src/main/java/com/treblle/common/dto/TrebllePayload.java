package com.treblle.common.dto;

public class TrebllePayload {

  private static final Integer TREBLLE_VERSION = 20;

  private String api_key;
  private String sdk_token;
  private String sdk;
  private Integer version = TREBLLE_VERSION;
  private Data data;

  public String getApi_key() {
    return api_key;
  }

  public void setApi_key(String api_key) {
    this.api_key = api_key;
  }

  public String getSdk_token() {
    return sdk_token;
  }

  public void setSdk_token(String sdk_token) {
    this.sdk_token = sdk_token;
  }

  public String getSdk() {
    return sdk;
  }

  public void setSdk(String sdk) {
    this.sdk = sdk;
  }

  public Integer getVersion() {
    return version;
  }

  public void setVersion(Integer version) {
    this.version = version;
  }

  public Data getData() {
    return data;
  }

  public void setData(Data data) {
    this.data = data;
  }

}
