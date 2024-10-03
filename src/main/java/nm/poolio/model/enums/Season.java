package nm.poolio.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum Season {
  S_2024("2024");

  @Getter final String display;

  public static Season getCurrent() {
    return S_2024;
  }
}
