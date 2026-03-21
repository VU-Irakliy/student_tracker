package com.studio.app.repository;

import com.studio.app.entity.Student;
import com.studio.app.entity.Payer;
import com.studio.app.enums.Currency;
import com.studio.app.enums.PricingType;
import com.studio.app.enums.StudentClassType;
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
                .pricingType(PricingType.PER_CLASS)
                .pricePerClass(new BigDecimal("35.00"))
                .currency(Currency.EUROS)
                .timezone(StudioTimezone.SPAIN)
                .classType(StudentClassType.EGE)
                .debtor(true)
                .build());

        deletedStudent = Student.builder()
                .firstName("Deleted")
                .lastName("User")
                .pricingType(PricingType.PER_CLASS)
                .pricePerClass(new BigDecimal("25.00"))
                .currency(Currency.DOLLARS)
                .timezone(StudioTimezone.SPAIN)
                .classType(StudentClassType.CASUAL)
                .build();
        deletedStudent.setDeleted(true);
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
                .hasValueSatisfying(s -> assertThat(s.getFirstName()).isEqualTo("Ana"));
    }

    @Test
    void findByIdAndDeletedFalse_returnsEmptyForDeletedStudent() {
        assertThat(studentRepository.findByIdAndDeletedFalse(deletedStudent.getId()))
                .isEmpty();
    }

    @Test
    void findByIdAndDeletedFalse_returnsEmptyForNonExistentId() {
        assertThat(studentRepository.findByIdAndDeletedFalse(9999L))
                .isEmpty();
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

    @Test
    void searchByName_partialMatch() {
        List<Student> result = studentRepository.searchByName("Gar");

        assertThat(result).hasSize(1);
    }

    @Test
    void searchByNameAndDebtor_appliesBothFilters() {
        em.persistAndFlush(Student.builder()
                .firstName("Ana")
                .lastName("Petrova")
                .pricingType(PricingType.PER_CLASS)
                .pricePerClass(new BigDecimal("20.00"))
                .currency(Currency.EUROS)
                .timezone(StudioTimezone.SPAIN)
                .classType(StudentClassType.CASUAL)
                .debtor(false)
                .build());

        List<Student> result = studentRepository.searchByNameAndDebtor("ana", true);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(activeStudent.getId());
    }

    @Test
    void findAllByDeletedFalse_returnsMultipleActiveStudents() {
        em.persistAndFlush(Student.builder()
                .firstName("Ivan").lastName("Petrov")
                .pricingType(PricingType.PACKAGE)
                .currency(Currency.RUBLES)
                .timezone(StudioTimezone.RUSSIA_MOSCOW)
                .classType(StudentClassType.OGE)
                .build());

        List<Student> result = studentRepository.findAllByDeletedFalse();
        assertThat(result).hasSize(2);
    }

    @Test
    void findAllByDeletedFalseAndDebtor_returnsOnlyDebtors() {
        em.persistAndFlush(Student.builder()
                .firstName("Ivan").lastName("Petrov")
                .pricingType(PricingType.PER_CLASS)
                .pricePerClass(new BigDecimal("20.00"))
                .currency(Currency.EUROS)
                .timezone(StudioTimezone.SPAIN)
                .classType(StudentClassType.CASUAL)
                .debtor(false)
                .build());

        List<Student> result = studentRepository.findAllByDeletedFalseAndDebtor(true);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(activeStudent.getId());
    }

    @Test
    void findAllByDeletedFalseAndPricingType_returnsOnlyMatchingPricingType() {
        em.persistAndFlush(Student.builder()
                .firstName("Ivan").lastName("Petrov")
                .pricingType(PricingType.PACKAGE)
                .currency(Currency.RUBLES)
                .timezone(StudioTimezone.RUSSIA_MOSCOW)
                .classType(StudentClassType.OGE)
                .build());

        List<Student> result = studentRepository.findAllByDeletedFalseAndPricingType(PricingType.PACKAGE);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getFirstName()).isEqualTo("Ivan");
    }

    @Test
    void searchByNameAndDebtorAndPricingType_appliesAllFilters() {
        em.persistAndFlush(Student.builder()
                .firstName("Ana")
                .lastName("Package")
                .pricingType(PricingType.PACKAGE)
                .currency(Currency.EUROS)
                .timezone(StudioTimezone.SPAIN)
                .classType(StudentClassType.CASUAL)
                .debtor(true)
                .build());

        List<Student> result = studentRepository.searchByNameAndDebtorAndPricingType("ana", true, PricingType.PACKAGE);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getLastName()).isEqualTo("Package");
    }

    @Test
    void activeStudent_hasCurrencySet() {
        var student = studentRepository.findByIdAndDeletedFalse(activeStudent.getId()).orElseThrow();
        assertThat(student.getCurrency()).isEqualTo(Currency.EUROS);
    }

    @Test
    void activeStudent_hasPricingTypeSet() {
        var student = studentRepository.findByIdAndDeletedFalse(activeStudent.getId()).orElseThrow();
        assertThat(student.getPricingType()).isEqualTo(PricingType.PER_CLASS);
    }

    @Test
    void activeStudent_hasClassTypeSet() {
        var student = studentRepository.findByIdAndDeletedFalse(activeStudent.getId()).orElseThrow();
        assertThat(student.getClassType()).isEqualTo(StudentClassType.EGE);
    }

    @Test
    void searchByStudentOrPayerName_matchesStudentFullName() {
        List<Student> result = studentRepository.searchByStudentOrPayerName("Ana Garcia");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(activeStudent.getId());
    }

    @Test
    void searchByStudentOrPayerName_matchesActivePayerName() {
        em.persistAndFlush(Payer.builder()
                .student(activeStudent)
                .fullName("Maria Garcia")
                .phoneNumber("+34600123456")
                .build());

        List<Student> result = studentRepository.searchByStudentOrPayerName("maria");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(activeStudent.getId());
    }

    @Test
    void searchByStudentOrPayerName_ignoresDeletedPayerRows() {
        var deletedPayer = Payer.builder()
                .student(activeStudent)
                .fullName("Hidden Parent")
                .build();
        deletedPayer.setDeleted(true);
        em.persistAndFlush(deletedPayer);

        List<Student> result = studentRepository.searchByStudentOrPayerName("hidden");

        assertThat(result).isEmpty();
    }

    @Test
    void searchByStudentOrPayerName_returnsDistinctStudentWhenMultiplePayersMatch() {
        em.persistAndFlush(Payer.builder().student(activeStudent).fullName("Alpha Parent").build());
        em.persistAndFlush(Payer.builder().student(activeStudent).fullName("Alpha Guardian").build());

        List<Student> result = studentRepository.searchByStudentOrPayerName("alpha");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(activeStudent.getId());
    }
}

