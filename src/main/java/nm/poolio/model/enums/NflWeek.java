package nm.poolio.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum NflWeek {
  WEEK_0(0),
  WEEK_1(1),
  WEEK_2(2),
  WEEK_3(3),
  WEEK_4(4),
  WEEK_5(5),
  WEEK_6(6),
  WEEK_7(7),
  WEEK_8(8),
  WEEK_9(9),
  WEEK_10(10),
  WEEK_11(11),
  WEEK_12(12),
  WEEK_13(13),
  WEEK_14(14),
  WEEK_15(15),
  WEEK_16(16),
  WEEK_17(17),
  WEEK_18(18),
  WILD_CARD(19),
  DIVISIONAL(20),
  CHAMPIONSHIP(21),
  SUPER_BOWL(22);

  final int weekNum;

  public static NflWeek findByWeekNum(int weekNum) {
    return Arrays.stream(NflWeek.values())
        .filter(week -> week.weekNum == weekNum)
        .findFirst()
        .orElse(null);
  }
}
