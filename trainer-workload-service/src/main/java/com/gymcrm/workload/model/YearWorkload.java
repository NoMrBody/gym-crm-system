package com.gymcrm.workload.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class YearWorkload {
    @Min(1970)
    @Max(9999)
    private int year;

    @Valid
    private List<MonthlyWorkload> months = new ArrayList<>();
}
