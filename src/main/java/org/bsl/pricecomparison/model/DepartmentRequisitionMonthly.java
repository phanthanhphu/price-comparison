package org.bsl.pricecomparison.model;

public class DepartmentRequisitionMonthly {

    private String name;   // Tên phòng ban
    private String value;  // Giá trị liên quan đến phòng ban
    private int id;        // ID của phòng ban

    public DepartmentRequisitionMonthly() {
    }

    public DepartmentRequisitionMonthly(String name, String value, int id) {
        this.name = name;
        this.value = value;
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
