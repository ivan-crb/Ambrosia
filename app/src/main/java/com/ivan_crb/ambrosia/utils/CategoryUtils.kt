package com.ivan_crb.ambrosia.utils

object CategoryUtils {
    private val categoriesMap = mapOf(
        "Produce" to listOf(
            "apple", "banana", "orange", "carrot", "broccoli", "spinach", "lettuce", "tomato", "onion", "garlic", "potato", "pepper", "cucumber", "lemon", "lime", "berry", "strawberry", "blueberry", "raspberry", "grape", "avocado", "mushroom", "kale", "cabbage", "zucchini", "eggplant", "asparagus", "celery", "corn", "ginger", "herb", "parsley", "cilantro", "basil", "mint", "rosemary", "thyme",
            "manzana", "platano", "naranja", "zanahoria", "brócoli", "espinaca", "lechuga", "tomate", "cebolla", "ajo", "patata", "pimiento", "pepino", "limón", "lima", "baya", "fresa", "arándano", "frambuesa", "uva", "aguacate", "seta", "col", "calabacín", "berenjena", "espárrago", "apio", "maíz", "jengibre", "hierba", "perejil", "cilantro", "albahaca", "menta", "romero", "tomillo"
        ),
        "Dairy" to listOf(
            "milk", "cheese", "yogurt", "butter", "cream", "egg", "margarine", "sour cream", "cottage cheese", "parmesan", "mozzarella", "cheddar",
            "leche", "queso", "yogur", "mantequilla", "crema", "nata", "huevo", "margarina", "queso crema", "parmesano", "mozzarella", "cheddar"
        ),
        "Meat" to listOf(
            "chicken", "beef", "pork", "lamb", "turkey", "bacon", "sausage", "ham", "steak", "mince", "breast", "thigh", "wing", "fish", "salmon", "tuna", "shrimp", "prawn", "cod", "trout",
            "pollo", "ternera", "cerdo", "cordero", "pavo", "beicon", "salchicha", "jamón", "filete", "picada", "pechuga", "muslo", "ala", "pescado", "salmón", "atún", "gamba", "langostino", "bacalao", "trucha"
        ),
        "Pantry" to listOf(
            "flour", "sugar", "salt", "oil", "vinegar", "rice", "pasta", "spaghetti", "noodle", "honey", "syrup", "baking powder", "baking soda", "yeast", "canned", "bean", "lentil", "chickpea", "spice", "cinnamon", "pepper", "stock", "broth", "sauce", "ketchup", "mayonnaise", "mustard", "nut", "almond", "walnut", "peanut", "seed", "quinoa", "oat", "cereal",
            "harina", "azúcar", "sal", "aceite", "vinagre", "arroz", "pasta", "espagueti", "fideo", "miel", "jarabe", "levadura", "lenteja", "garbanzo", "especia", "canela", "pimienta", "caldo", "salsa", "ketchup", "mayonesa", "mostaza", "fruto seco", "almendra", "nuez", "cacahuete", "semilla", "quinoa", "avena", "cereal"
        ),
        "Frozen" to listOf(
            "frozen", "ice cream", "pizza", "vegetable mix", "fruit mix",
            "congelado", "helado", "pizza", "mezcla de verduras", "mezcla de frutas"
        ),
        "Bakery" to listOf(
            "bread", "bun", "roll", "baguette", "tortilla", "pita", "pastry", "cake", "cookie", "muffin",
            "pan", "bollo", "baguette", "tortilla", "pita", "pastelería", "tarta", "galleta", "magdalena"
        )
    )

    fun suggestCategory(name: String): String {
        val lowerName = name.lowercase().trim()
        if (lowerName.isEmpty()) return "Other"
        
        for ((category, keywords) in categoriesMap) {
            if (keywords.any { lowerName.contains(it) }) {
                return category
            }
        }
        return "Other"
    }
}
