package com.simplyviral.provider.repository;

import com.simplyviral.provider.entity.PromptTemplate;
import com.simplyviral.shared.constant.StepKey;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PromptTemplateRepository extends JpaRepository<PromptTemplate, StepKey> {
}
