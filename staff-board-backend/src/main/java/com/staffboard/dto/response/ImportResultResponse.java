package com.staffboard.dto.response;

import lombok.*;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ImportResultResponse {
    private int totalCount;
    private int successCount;
    private int failCount;
    private List<String> errors;
}
