package com.saas.saascommon.model;

public interface IBusinessEntity<ID> {
    ID getId();
    void setId(ID id);

    String getCode();
    void setCode(String code);
}
