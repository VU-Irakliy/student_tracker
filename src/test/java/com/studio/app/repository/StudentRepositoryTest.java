package com.studio.app.repository;

import com.studio.app.entity.Student;
import com.studio.app.enums.PricingType;
import com.studio.app.enums.StudioTimezone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class StudentRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private StudentRepository studentRepository;

    private Student activeStudent;
    private Student deletedStudent;

    @BeforeEach
    void setUp() {
        activeStudent = em.persistAndFlush(Student.builder()
                .firstName("Ana")
                .lastName("Garcia")
                .email("ana@test.com")
                .pricingType(PricingType.PER_CLASS)
                .pricePerClass(new BigDecimal("35.00"))
                .timezone(StudioTimezone.SPAIN)
                .build());

        deletedStudent = Student.builder()
                .firstName("Deleted")
                .lastName("User")
                .email("deleted@test.com")
                .pricingType(PricingType.PER_CLASS)
                .pricePerClass(new BigDecimal("25.00"))
                .timezone(StudioTimezone.SPAIN)
                .deleted(true)
                .build();
        em.persistAndFlush(deletedStudent);
    }

    @Test
    void findAllByDeletedFalse_excludesDeletedStudents() {
        List<Student> result = studentRepository.findAllByDeletedFalse();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getFirstName()).isEqualTo("Ana");
    }

    @Test
    void findByIdAndDeletedFalse_returnsActiveStudent() {
        assertThat(studentRepository.findByIdAndDeletedFalse(activeStudent.getId()))
                .isPresent()
                .hasValueSatisfying(s -> assertThat(s.getEmail()).isEqualTo("ana@test.com"));
    }

    @Test
    void findByIdAndDeletedFalse_returnsEmptyForDeletedStudent() {
        assertThat(studentRepository.findByIdAndDeletedFalse(deletedStudent.getId()))
                .isEmpty();
    }

    @Test
    void existsByEmailAndDeletedFalse_trueForActiveStudent() {
        assertThat(studentRepository.existsByEmailAndDeletedFalse("ana@test.com")).isTrue();
    }

    @Test
    void existsByEmailAndDeletedFalse_falseForDeletedStudent() {
        assertThat(studentRepository.existsByEmailAndDeletedFalse("deleted@test.com")).isFalse();
    }

    @Test
    void existsByEmailAndDeletedFalse_falseForUnknownEmail() {
        assertThat(studentRepository.existsByEmailAndDeletedFalse("unknown@test.com")).isFalse();
    }

    @Test
    void searchByName_caseInsensitiveFirstName() {
        List<Student> result = studentRepository.searchByName("ana");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getFirstName()).isEqualTo("Ana");
    }

    @Test
    void searchByName_caseInsensitiveLastName() {
        List<Student> result = studentRepository.searchByName("GARCIA");

        assertThat(result).hasSize(1);
    }

    @Test
    void searchByName_excludesDeletedStudents() {
        List<Student> result = studentRepository.searchByName("Deleted");

        assertThat(result).isEmpty();
    }

    @Test
    void searchByName_noMatch() {
        List<Student> result = studentRepository.searchByName("xyz");

        assertThat(result).isEmpty();
    }
}

