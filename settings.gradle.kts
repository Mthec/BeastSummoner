rootProject.name = "BeastSummoner"
include(":BMLBuilder", ":CreatureCustomiser", ":PlaceNpc", ":QuestionLibrary", ":TradeLibrary", ":WurmTestingHelper")
project(":BMLBuilder").projectDir = file("../BMLBuilder")
project(":CreatureCustomiser").projectDir = file("../CreatureCustomiser")
project(":PlaceNpc").projectDir = file("../PlaceNpc")
project(":QuestionLibrary").projectDir = file("../QuestionLibrary")
project(":WurmTestingHelper").projectDir = file("../WurmTestingHelper")
project(":TradeLibrary").projectDir = file("../TradeLibrary")

