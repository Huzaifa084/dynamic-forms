package com.apex.payroll.model.masterdata;

import lombok.Getter;

@Getter
public enum MdFkAction {
    NO_ACTION("NO ACTION"),
    CASCADE("CASCADE"),
    SET_NULL("SET NULL"),
    SET_DEFAULT("SET DEFAULT"),
    RESTRICT("RESTRICT");

    private final String sqlKeyword;

    MdFkAction(String sqlKeyword) {
        this.sqlKeyword = sqlKeyword;
    }

}

