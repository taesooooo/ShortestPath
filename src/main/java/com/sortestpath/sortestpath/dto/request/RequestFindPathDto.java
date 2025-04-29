package com.sortestpath.sortestpath.dto.request;

import com.sortestpath.sortestpath.common.validation.ValidCoordinate;
import com.sortestpath.sortestpath.core.pathengine.Coordinate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class RequestFindPathDto {
	@NotBlank(message = "시작 좌표가 없습니다.")
	@ValidCoordinate
	private String start;
	@NotBlank(message = "종료 좌표가 없습니다.")
	@ValidCoordinate
	private String end;
}
