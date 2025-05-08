package org.example.expert.domain.todo.spec;

import org.example.expert.domain.todo.entity.Todo;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

public class TodoSpecs {
    // 날씨가 일치 하는지 필터링
    public static Specification<Todo> equalWeather(String weather){
        return (root, query, criteriaBuilder) ->
                weather == null ? null : criteriaBuilder.equal(root.get("weather"), weather);
    }
    // 특정 날짜/시간 같거나 이후에 수정된 항목 필터링
    public static Specification<Todo> updatedAfter(LocalDateTime start){
        return (root, query, criteriaBuilder)->
                start == null ? null : criteriaBuilder.greaterThanOrEqualTo(root.get("updatedAfter"), start);
    }
    // 특정 날짜/시간 같거나 이전에 수정된 항목 필터링
    public static Specification<Todo> updatedBefore(LocalDateTime end){
        return (root, query, criteriaBuilder)->
                end == null ? null : criteriaBuilder.greaterThanOrEqualTo(root.get("updatedBefore"), end);
    }
}
