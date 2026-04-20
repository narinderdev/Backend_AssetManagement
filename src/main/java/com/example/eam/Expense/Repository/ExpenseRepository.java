package com.example.eam.Expense.Repository;

import com.example.eam.Expense.Entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {
}
