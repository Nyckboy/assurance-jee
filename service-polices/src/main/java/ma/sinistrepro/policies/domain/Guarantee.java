package ma.sinistrepro.policies.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "guarantees")
public class Guarantee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_id", nullable = false)
    private Policy policy;

    @Column(nullable = false)
    private String libelle;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal plafond;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal franchise;

    protected Guarantee() {
    }

    public Guarantee(String libelle, BigDecimal plafond, BigDecimal franchise) {
        this.libelle = libelle;
        this.plafond = plafond;
        this.franchise = franchise;
    }

    public Long getId() {
        return id;
    }

    public String getLibelle() {
        return libelle;
    }

    public BigDecimal getPlafond() {
        return plafond;
    }

    public BigDecimal getFranchise() {
        return franchise;
    }

    public void setPolicy(Policy policy) {
        this.policy = policy;
    }

    public void update(String libelle, BigDecimal plafond, BigDecimal franchise) {
        this.libelle = libelle;
        this.plafond = plafond;
        this.franchise = franchise;
    }
}
