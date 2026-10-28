package com.gym.trainer;

import com.gym.trainer.entity.Trainer;
import com.gym.trainer.repository.TrainerRepository;
import com.gym.trainer.service.TrainerService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TrainerFunctionalityTest {
    @Test void pagedTrainerSearchAppliesFiltersAndKeepsPageMetadata() {
        TrainerRepository repository=mock(TrainerRepository.class);
        TrainerService service=new TrainerService(repository);
        Trainer trainer=new Trainer();trainer.setName("Coach");
        var pageable=PageRequest.of(1,10,Sort.by("name"));
        when(repository.findAll(any(Specification.class),eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(trainer),pageable,21));
        var result=service.getTrainersPage("coach","strength","available","active",pageable);
        assertEquals(21,result.getTotalElements());
        assertEquals(1,result.getNumber());
        assertEquals(List.of(trainer),result.getContent());
        verify(repository).findAll(any(Specification.class),eq(pageable));
    }
}
