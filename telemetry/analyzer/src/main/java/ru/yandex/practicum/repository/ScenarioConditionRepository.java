package ru.yandex.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.model.ScenarioCondition;
import ru.yandex.practicum.model.ScenarioConditionId;

@Repository
public interface ScenarioConditionRepository extends JpaRepository<ScenarioCondition, ScenarioConditionId> {
    void deleteByScenarioId(Long scenarioId);
}