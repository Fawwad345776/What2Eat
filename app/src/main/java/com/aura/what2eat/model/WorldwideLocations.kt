package com.aura.what2eat.model

data class QuickLocation(
    val flag: String,
    val country: String,
    val cities: List<String>
)

object WorldwideLocations {

    val POPULAR_LOCATIONS: List<QuickLocation> = listOf(
        // Asia & South Asia
        QuickLocation("🇵🇰", "Pakistan", listOf("Karachi", "Lahore", "Islamabad", "Rawalpindi", "Faisalabad", "Multan", "Peshawar", "Quetta", "Sialkot", "Gujranwala", "Hyderabad", "Sukkur", "Abbottabad", "Bahawalpur")),
        QuickLocation("🇮🇳", "India", listOf("Delhi", "Mumbai", "Bengaluru", "Hyderabad", "Chennai", "Kolkata", "Pune", "Ahmedabad", "Jaipur", "Lucknow", "Chandigarh", "Surat", "Indore", "Kochi")),
        QuickLocation("🇧🇩", "Bangladesh", listOf("Dhaka", "Chittagong", "Sylhet", "Rajshahi", "Khulna", "Barisal", "Comilla")),
        QuickLocation("🇱🇰", "Sri Lanka", listOf("Colombo", "Kandy", "Galle", "Jaffna", "Negombo")),
        QuickLocation("🇳🇵", "Nepal", listOf("Kathmandu", "Pokhara", "Lalitpur", "Biratnagar")),
        QuickLocation("🇦🇫", "Afghanistan", listOf("Kabul", "Herat", "Mazar-i-Sharif", "Kandahar")),

        // Middle East & GCC
        QuickLocation("🇦🇪", "UAE", listOf("Dubai", "Abu Dhabi", "Sharjah", "Ajman", "Ras Al Khaimah", "Al Ain", "Fujairah")),
        QuickLocation("🇸🇦", "Saudi Arabia", listOf("Riyadh", "Jeddah", "Makkah", "Madinah", "Dammam", "Khobar", "Tabuk", "Taif", "Abha", "Jubail")),
        QuickLocation("🇶🇦", "Qatar", listOf("Doha", "Al Rayyan", "Al Wakrah", "Al Khor", "Umm Salal")),
        QuickLocation("🇰🇼", "Kuwait", listOf("Kuwait City", "Hawally", "Salmiya", "Al Jahra", "Al Ahmadi")),
        QuickLocation("🇧🇭", "Bahrain", listOf("Manama", "Riffa", "Muharraq", "Hamad Town", "A'ali")),
        QuickLocation("🇴🇲", "Oman", listOf("Muscat", "Salalah", "Sohar", "Nizwa", "Sur")),
        QuickLocation("🇹🇷", "Turkey", listOf("Istanbul", "Ankara", "Izmir", "Antalya", "Bursa", "Adana", "Gaziantep", "Konya")),
        QuickLocation("🇯🇴", "Jordan", listOf("Amman", "Zarqa", "Irbid", "Aqaba")),
        QuickLocation("🇱🇧", "Lebanon", listOf("Beirut", "Tripoli", "Sidon", "Jounieh")),
        QuickLocation("🇮🇶", "Iraq", listOf("Baghdad", "Erbil", "Basra", "Mosul", "Sulaymaniyah")),
        QuickLocation("🇮🇷", "Iran", listOf("Tehran", "Mashhad", "Isfahan", "Shiraz", "Tabriz")),

        // North America
        QuickLocation("🇺🇸", "USA", listOf("New York", "Los Angeles", "Chicago", "Houston", "Dallas", "San Francisco", "Miami", "Seattle", "Atlanta", "Boston", "Washington DC", "Austin", "Denver", "Phoenix")),
        QuickLocation("🇨🇦", "Canada", listOf("Toronto", "Vancouver", "Montreal", "Calgary", "Ottawa", "Edmonton", "Winnipeg", "Quebec City", "Mississauga", "Halifax")),
        QuickLocation("🇲🇽", "Mexico", listOf("Mexico City", "Guadalajara", "Monterrey", "Cancun", "Puebla", "Tijuana")),

        // Europe
        QuickLocation("🇬🇧", "UK", listOf("London", "Manchester", "Birmingham", "Leeds", "Glasgow", "Edinburgh", "Liverpool", "Bristol", "Cardiff", "Belfast")),
        QuickLocation("🇩🇪", "Germany", listOf("Berlin", "Munich", "Frankfurt", "Hamburg", "Cologne", "Stuttgart", "Düsseldorf", "Dortmund", "Leipzig")),
        QuickLocation("🇫🇷", "France", listOf("Paris", "Lyon", "Marseille", "Nice", "Toulouse", "Bordeaux", "Strasbourg", "Lille", "Nantes")),
        QuickLocation("🇮🇹", "Italy", listOf("Rome", "Milan", "Naples", "Florence", "Venice", "Turin", "Bologna", "Palermo")),
        QuickLocation("🇪🇸", "Spain", listOf("Madrid", "Barcelona", "Valencia", "Seville", "Zaragoza", "Malaga", "Bilbao")),
        QuickLocation("🇳🇱", "Netherlands", listOf("Amsterdam", "Rotterdam", "The Hague", "Utrecht", "Eindhoven", "Groningen")),
        QuickLocation("🇨🇭", "Switzerland", listOf("Zurich", "Geneva", "Basel", "Bern", "Lausanne", "Lucerne")),
        QuickLocation("🇸🇪", "Sweden", listOf("Stockholm", "Gothenburg", "Malmö", "Uppsala")),
        QuickLocation("🇳🇴", "Norway", listOf("Oslo", "Bergen", "Trondheim", "Stavanger")),
        QuickLocation("🇩🇰", "Denmark", listOf("Copenhagen", "Aarhus", "Odense", "Aalborg")),
        QuickLocation("🇫🇮", "Finland", listOf("Helsinki", "Espoo", "Tampere", "Vantaa")),
        QuickLocation("🇮🇪", "Ireland", listOf("Dublin", "Cork", "Galway", "Limerick")),
        QuickLocation("🇧🇪", "Belgium", listOf("Brussels", "Antwerp", "Ghent", "Bruges", "Liege")),
        QuickLocation("🇦🇹", "Austria", listOf("Vienna", "Salzburg", "Innsbruck", "Graz", "Linz")),
        QuickLocation("🇵🇹", "Portugal", listOf("Lisbon", "Porto", "Braga", "Coimbra", "Faro")),
        QuickLocation("🇬🇷", "Greece", listOf("Athens", "Thessaloniki", "Heraklion", "Patras")),
        QuickLocation("🇵🇱", "Poland", listOf("Warsaw", "Krakow", "Wroclaw", "Gdansk", "Poznan")),
        QuickLocation("🇨🇿", "Czech Republic", listOf("Prague", "Brno", "Ostrava", "Plzen")),
        QuickLocation("🇭🇺", "Hungary", listOf("Budapest", "Debrecen", "Szeged")),
        QuickLocation("🇷🇴", "Romania", listOf("Bucharest", "Cluj-Napoca", "Timisoara", "Iasi")),
        QuickLocation("🇷🇺", "Russia", listOf("Moscow", "Saint Petersburg", "Novosibirsk", "Yekaterinburg", "Kazan")),
        QuickLocation("🇺🇦", "Ukraine", listOf("Kyiv", "Lviv", "Odesa", "Kharkiv")),

        // East & Southeast Asia
        QuickLocation("🇲🇾", "Malaysia", listOf("Kuala Lumpur", "Penang", "Johor Bahru", "Ipoh", "Melaka", "Kota Kinabalu")),
        QuickLocation("🇸🇬", "Singapore", listOf("Singapore")),
        QuickLocation("🇮🇩", "Indonesia", listOf("Jakarta", "Surabaya", "Bandung", "Medan", "Bali", "Semarang", "Yogyakarta")),
        QuickLocation("🇹🇭", "Thailand", listOf("Bangkok", "Chiang Mai", "Phuket", "Pattaya", "Nonthaburi")),
        QuickLocation("🇵🇭", "Philippines", listOf("Manila", "Quezon City", "Cebu City", "Davao", "Taguig")),
        QuickLocation("🇯🇵", "Japan", listOf("Tokyo", "Osaka", "Kyoto", "Yokohama", "Sapporo", "Fukuoka", "Nagoya")),
        QuickLocation("🇰🇷", "South Korea", listOf("Seoul", "Busan", "Incheon", "Daegu", "Daejeon")),
        QuickLocation("🇨🇳", "China", listOf("Beijing", "Shanghai", "Guangzhou", "Shenzhen", "Chengdu", "Hangzhou", "Wuhan")),
        QuickLocation("🇻🇳", "Vietnam", listOf("Ho Chi Minh City", "Hanoi", "Da Nang", "Hai Phong")),

        // Oceania
        QuickLocation("🇦🇺", "Australia", listOf("Sydney", "Melbourne", "Brisbane", "Perth", "Adelaide", "Gold Coast", "Canberra", "Newcastle")),
        QuickLocation("🇳🇿", "New Zealand", listOf("Auckland", "Wellington", "Christchurch", "Hamilton", "Tauranga")),

        // Africa
        QuickLocation("🇪🇬", "Egypt", listOf("Cairo", "Alexandria", "Giza", "Sharm El Sheikh", "Hurghada", "Luxor")),
        QuickLocation("🇿🇦", "South Africa", listOf("Johannesburg", "Cape Town", "Durban", "Pretoria", "Port Elizabeth")),
        QuickLocation("🇲🇦", "Morocco", listOf("Casablanca", "Marrakech", "Rabat", "Tangier", "Fes")),
        QuickLocation("🇳🇬", "Nigeria", listOf("Lagos", "Abuja", "Kano", "Ibadan", "Port Harcourt")),
        QuickLocation("🇰🇪", "Kenya", listOf("Nairobi", "Mombasa", "Kisumu", "Nakuru")),
        QuickLocation("🇩🇿", "Algeria", listOf("Algiers", "Oran", "Constantine")),
        QuickLocation("🇹🇳", "Tunisia", listOf("Tunis", "Sfax", "Sousse")),

        // South & Central America
        QuickLocation("🇧🇷", "Brazil", listOf("São Paulo", "Rio de Janeiro", "Brasília", "Salvador", "Fortaleza", "Belo Horizonte")),
        QuickLocation("🇦🇷", "Argentina", listOf("Buenos Aires", "Cordoba", "Rosario", "Mendoza")),
        QuickLocation("🇨🇴", "Colombia", listOf("Bogota", "Medellin", "Cali", "Barranquilla")),
        QuickLocation("🇨🇱", "Chile", listOf("Santiago", "Valparaiso", "Concepcion")),
        QuickLocation("🇵🇪", "Peru", listOf("Lima", "Arequipa", "Cusco"))
    )

    val ALL_WORLD_COUNTRIES: List<Pair<String, String>> = listOf(
        Pair("🇦🇫", "Afghanistan"), Pair("🇦🇱", "Albania"), Pair("🇩🇿", "Algeria"), Pair("🇦🇩", "Andorra"),
        Pair("🇦🇴", "Angola"), Pair("🇦🇷", "Argentina"), Pair("🇦🇲", "Armenia"), Pair("🇦🇺", "Australia"),
        Pair("🇦🇹", "Austria"), Pair("🇦🇿", "Azerbaijan"), Pair("🇧🇸", "Bahamas"), Pair("🇧🇭", "Bahrain"),
        Pair("🇧🇩", "Bangladesh"), Pair("🇧🇧", "Barbados"), Pair("🇧🇾", "Belarus"), Pair("🇧🇪", "Belgium"),
        Pair("🇧🇿", "Belize"), Pair("🇧🇯", "Benin"), Pair("🇧🇹", "Bhutan"), Pair("🇧🇴", "Bolivia"),
        Pair("🇧🇦", "Bosnia and Herzegovina"), Pair("🇧🇼", "Botswana"), Pair("🇧🇷", "Brazil"), Pair("🇧🇳", "Brunei"),
        Pair("🇧🇬", "Bulgaria"), Pair("🇧🇫", "Burkina Faso"), Pair("🇧🇮", "Burundi"), Pair("🇰🇭", "Cambodia"),
        Pair("🇨🇲", "Cameroon"), Pair("🇨🇦", "Canada"), Pair("🇨🇱", "Chile"), Pair("🇨🇳", "China"),
        Pair("🇨🇴", "Colombia"), Pair("🇨🇷", "Costa Rica"), Pair("🇭🇷", "Croatia"), Pair("🇨🇺", "Cuba"),
        Pair("🇨🇾", "Cyprus"), Pair("🇨🇿", "Czech Republic"), Pair("🇩🇰", "Denmark"), Pair("🇩🇯", "Djibouti"),
        Pair("🇩🇴", "Dominican Republic"), Pair("🇪🇨", "Ecuador"), Pair("🇪🇬", "Egypt"), Pair("🇸🇻", "El Salvador"),
        Pair("🇪🇪", "Estonia"), Pair("🇪🇹", "Ethiopia"), Pair("🇫🇯", "Fiji"), Pair("🇫🇮", "Finland"),
        Pair("🇫🇷", "France"), Pair("🇬🇦", "Gabon"), Pair("🇬🇲", "Gambia"), Pair("🇬🇪", "Georgia"),
        Pair("🇩🇪", "Germany"), Pair("🇬🇭", "Ghana"), Pair("🇬🇷", "Greece"), Pair("🇬🇹", "Guatemala"),
        Pair("🇭🇳", "Honduras"), Pair("🇭🇺", "Hungary"), Pair("🇮🇸", "Iceland"), Pair("🇮🇳", "India"),
        Pair("🇮🇩", "Indonesia"), Pair("🇮🇷", "Iran"), Pair("🇮🇶", "Iraq"), Pair("🇮🇪", "Ireland"),
        Pair("🇮🇱", "Israel"), Pair("🇮🇹", "Italy"), Pair("🇯🇲", "Jamaica"), Pair("🇯🇵", "Japan"),
        Pair("🇯🇴", "Jordan"), Pair("🇰🇿", "Kazakhstan"), Pair("🇰🇪", "Kenya"), Pair("🇰🇼", "Kuwait"),
        Pair("🇰🇬", "Kyrgyzstan"), Pair("🇱🇦", "Laos"), Pair("🇱🇻", "Latvia"), Pair("🇱🇧", "Lebanon"),
        Pair("🇱🇾", "Libya"), Pair("🇱🇹", "Lithuania"), Pair("🇱🇺", "Luxembourg"), Pair("🇲🇾", "Malaysia"),
        Pair("🇲🇻", "Maldives"), Pair("🇲🇱", "Mali"), Pair("🇲🇹", "Malta"), Pair("🇲🇷", "Mauritania"),
        Pair("🇲🇺", "Mauritius"), Pair("🇲🇽", "Mexico"), Pair("🇲🇩", "Moldova"), Pair("🇲🇨", "Monaco"),
        Pair("🇲🇳", "Mongolia"), Pair("🇲🇪", "Montenegro"), Pair("🇲🇦", "Morocco"), Pair("🇲🇿", "Mozambique"),
        Pair("🇲🇲", "Myanmar"), Pair("🇳🇦", "Namibia"), Pair("🇳🇵", "Nepal"), Pair("🇳🇱", "Netherlands"),
        Pair("🇳🇿", "New Zealand"), Pair("🇳🇮", "Nicaragua"), Pair("🇳🇬", "Nigeria"), Pair("🇳🇴", "Norway"),
        Pair("🇴🇲", "Oman"), Pair("🇵🇰", "Pakistan"), Pair("🇵🇸", "Palestine"), Pair("🇵🇦", "Panama"),
        Pair("🇵🇾", "Paraguay"), Pair("🇵🇪", "Peru"), Pair("🇵🇭", "Philippines"), Pair("🇵🇱", "Poland"),
        Pair("🇵🇹", "Portugal"), Pair("🇶🇦", "Qatar"), Pair("🇷🇴", "Romania"), Pair("🇷🇺", "Russia"),
        Pair("🇷🇼", "Rwanda"), Pair("🇸🇦", "Saudi Arabia"), Pair("🇸🇳", "Senegal"), Pair("🇷🇸", "Serbia"),
        Pair("🇸🇬", "Singapore"), Pair("🇸🇰", "Slovakia"), Pair("🇸🇮", "Slovenia"), Pair("🇸🇴", "Somalia"),
        Pair("🇿🇦", "South Africa"), Pair("🇰🇷", "South Korea"), Pair("🇪🇸", "Spain"), Pair("🇱🇰", "Sri Lanka"),
        Pair("🇸🇩", "Sudan"), Pair("🇸🇪", "Sweden"), Pair("🇨🇭", "Switzerland"), Pair("🇸🇾", "Syria"),
        Pair("🇹🇼", "Taiwan"), Pair("🇹🇯", "Tajikistan"), Pair("🇹🇿", "Tanzania"), Pair("🇹🇭", "Thailand"),
        Pair("🇹🇳", "Tunisia"), Pair("🇹🇷", "Turkey"), Pair("🇹🇲", "Turkmenistan"), Pair("🇺🇬", "Uganda"),
        Pair("🇺🇦", "Ukraine"), Pair("🇦🇪", "UAE"), Pair("🇬🇧", "UK"), Pair("🇺🇸", "USA"),
        Pair("🇺🇾", "Uruguay"), Pair("🇺🇿", "Uzbekistan"), Pair("🇻🇪", "Venezuela"), Pair("🇻🇳", "Vietnam"),
        Pair("🇾🇪", "Yemen"), Pair("🇿🇲", "Zambia"), Pair("🇿🇼", "Zimbabwe")
    )

    fun getCountryFlag(countryName: String): String {
        return ALL_WORLD_COUNTRIES.find { it.second.equals(countryName, ignoreCase = true) }?.first
            ?: POPULAR_LOCATIONS.find { it.country.equals(countryName, ignoreCase = true) }?.flag
            ?: "🌍"
    }

    fun getCitiesForCountry(countryName: String): List<String> {
        return POPULAR_LOCATIONS.find { it.country.equals(countryName, ignoreCase = true) }?.cities
            ?: emptyList()
    }
}
