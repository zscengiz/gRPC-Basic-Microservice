package com.zscengiz.discountservice.entity;

import lombok.*;

import javax.persistence.*;
import java.util.List;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name="categories")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String name;

    private String externalId;

    @OneToMany(mappedBy = "category", fetch = FetchType.LAZY)
    private List<Discount> discounts;

}
