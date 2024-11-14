package nm.poolio.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Season {
  S_2024("2024");

  final String display;

  public static Season getCurrent() {
    return S_2024;
  }
}
