package org.example.lamsserver.config;

import lombok.extern.slf4j.Slf4j;
import org.example.lamspojo.dto.CourseDTO;
import org.example.lamspojo.dto.LaboratoryDTO;
import org.example.lamspojo.po.UserLoginPO;
import org.example.lamspojo.vo.StudentVO;
import org.example.lamsserver.mapper.CourseMapper;
import org.example.lamsserver.mapper.LaboratoryMapper;
import org.example.lamsserver.mapper.UserMapper;
import org.example.lamsserver.service.CourseService;
import org.example.lamsserver.service.LaboratoryService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 首次启动时写入演示数据：管理员、两名学生、实验室与课程。
 * 只在对应表为空时执行，不会覆盖已有数据。
 */
@Component
@Slf4j
public class DataInitializer implements ApplicationRunner {

    private static final String DEFAULT_PASSWORD = "123456";

    private final UserMapper userMapper;
    private final LaboratoryMapper laboratoryMapper;
    private final CourseMapper courseMapper;
    private final CourseService courseService;
    private final LaboratoryService laboratoryService;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public DataInitializer(UserMapper userMapper,
                           LaboratoryMapper laboratoryMapper,
                           CourseMapper courseMapper,
                           CourseService courseService,
                           LaboratoryService laboratoryService) {
        this.userMapper = userMapper;
        this.laboratoryMapper = laboratoryMapper;
        this.courseMapper = courseMapper;
        this.courseService = courseService;
        this.laboratoryService = laboratoryService;
    }

    @Override
    public void run(ApplicationArguments args) {
        initUsers();
        initLaboratories();
        initCourses();
    }

    private void initUsers() {
        if (userMapper.countAll() > 0) {
            return;
        }
        createUser("admin", "系统管理员", "admin");
        createUser("student1", "张三", "student");
        createUser("student2", "李四", "student");
        log.info("已初始化演示账号：admin / student1 / student2，初始密码 {}", DEFAULT_PASSWORD);
    }

    private void createUser(String username, String realName, String userType) {
        UserLoginPO user = new UserLoginPO();
        user.setUsername(username);
        user.setPassword(encoder.encode(DEFAULT_PASSWORD));
        user.setRealName(realName);
        user.setUserType(userType);
        userMapper.insert(user);
    }

    private void initLaboratories() {
        if (!laboratoryMapper.list().isEmpty()) {
            return;
        }
        laboratoryService.save(lab("软件实验室", "配备 30 台开发机，用于软件类课程实验", "A", 101, 30));
        laboratoryService.save(lab("网络实验室", "配备路由交换设备与 30 台终端", "A", 102, 30));
        laboratoryService.save(lab("硬件实验室", "嵌入式与电路实验台 24 套", "B", 201, 24));
        laboratoryService.save(lab("人工智能实验室", "GPU 工作站 20 台", "B", 202, 20));
        log.info("已初始化 4 间实验室");
    }

    private LaboratoryDTO lab(String name, String description, String location, int roomNumber, int capacity) {
        LaboratoryDTO dto = new LaboratoryDTO();
        dto.setName(name);
        dto.setDescription(description);
        dto.setLocation(location);
        dto.setRoomNumber(roomNumber);
        dto.setCapacity(capacity);
        return dto;
    }

    private void initCourses() {
        if (!courseMapper.list().isEmpty()) {
            return;
        }
        List<Long> studentIds = new ArrayList<>();
        for (StudentVO student : userMapper.listStudents()) {
            studentIds.add(student.getId());
        }
        courseService.save(course("数据结构", "CS101", "2026-2027-1", 4, 8, 2, "线性表、树、图与经典算法", studentIds));
        courseService.save(course("计算机网络", "CS102", "2026-2027-1", 3, 8, 2, "TCP/IP 协议栈与网络编程", studentIds));
        courseService.save(course("操作系统", "CS103", "2026-2027-1", 4, 8, 2, "进程、内存与文件系统", studentIds));
        courseService.save(course("机器学习", "CS104", "2026-2027-1", 3, 6, 2, "监督学习与模型评估", studentIds));
        log.info("已初始化 4 门课程，并分配给 {} 名学生", studentIds.size());
    }

    private CourseDTO course(String name, String code, String semester, int credits,
                             int weeks, int periods, String description, List<Long> studentIds) {
        CourseDTO dto = new CourseDTO();
        dto.setName(name);
        dto.setCode(code);
        dto.setSemester(semester);
        dto.setCredits(credits);
        dto.setWeeks(weeks);
        dto.setPeriods(periods);
        dto.setDescription(description);
        dto.setStudentIds(studentIds);
        return dto;
    }
}
