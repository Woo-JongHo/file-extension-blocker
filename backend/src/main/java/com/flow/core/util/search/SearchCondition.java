package com.flow.core.util.search;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SearchCondition {
  private String publicKey;
  private Object value;
}
