package org.example.lamsserver.schedule;

import lombok.extern.slf4j.Slf4j;
import org.example.lamspojo.po.CoursePO;
import org.example.lamspojo.po.LaboratoryPO;
import org.example.lamsserver.mapper.CourseStudentMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 调用 Python OR-Tools 求解服务
 */
@Component
@Slf4j
public class PythonAlgoClient {

    private final RestTemplate restTemplate;
    private final CourseStudentMapper courseStudentMapper;
    private final String algoUrl;

    public PythonAlgoClient(@Value("${lams.schedule.algo-url}") String algoUrl,
                            @Value("${lams.schedule.timeout-ms:180000}") int timeoutMs,
                            CourseStudentMapper courseStudentMapper) {
        this.algoUrl = algoUrl;
        this.courseStudentMapper = courseStudentMapper;
        org.springframework.http.client.SimpleClientHttpRequestFactory factory =
                new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Duration.ofSeconds(10).toMillis());
        factory.setReadTimeout(timeoutMs);
        this.restTemplate = new RestTemplate(factory);
    }

    public List<ScheduleItem> solve(List<CoursePO> courses, List<LaboratoryPO> labs) {
        Map<String, Object> body = new LinkedHashMap<>();
        List<Map<String, Object>> coursePayload = new ArrayList<>();
        for (CoursePO c : courses) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("courseId", c.getId());
            m.put("courseName", c.getName());
            m.put("studentCount", c.getStudentCount() == null ? 0 : c.getStudentCount());
            m.put("weeks", c.getWeeks() == null ? 8 : c.getWeeks());
            m.put("sessionsPerWeek", c.getPeriods() == null ? 2 : c.getPeriods());
            // 选课学生名单随课程下传，算法据此保证同一学生同一节次不撞课
            m.put("studentIds", courseStudentMapper.listStudentIds(c.getId()));
            coursePayload.add(m);
        }
        List<Map<String, Object>> labPayload = new ArrayList<>();
        for (LaboratoryPO lab : labs) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("labId", lab.getId());
            m.put("labName", lab.getName());
            m.put("capacity", lab.getCapacity());
            labPayload.add(m);
        }
        body.put("courses", coursePayload);
        body.put("labs", labPayload);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        log.info("调用排课服务 {}，课程 {} 门，实验室 {} 间", algoUrl, coursePayload.size(), labPayload.size());
        try {
            ResponseEntity<List<ScheduleItem>> response = restTemplate.exchange(
                    algoUrl, HttpMethod.POST, request, new ParameterizedTypeReference<List<ScheduleItem>>() {
                    });
            List<ScheduleItem> items = response.getBody();
            if (items == null) {
                throw new IllegalStateException("排课服务返回空响应");
            }
            log.info("排课服务返回 {} 条结果", items.size());
            return items;
        } catch (HttpStatusCodeException e) {
            throw new IllegalStateException("排课服务返回错误(" + e.getRawStatusCode() + "): "
                    + e.getResponseBodyAsString(), e);
        } catch (RestClientException e) {
            throw new IllegalStateException("调用排课服务失败（请确认 Python 服务已在 " + algoUrl + " 启动）: "
                    + e.getMessage(), e);
        }
    }
}
