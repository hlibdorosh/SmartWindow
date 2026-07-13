package com.example.smartwindowsill

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight

@Composable
fun PlantListScreen() {
    val myPlants = listOf(
        "Фикус на подоконнике",
        "Кактус у монитора",
        "Мамина Герань",
        "Алоэ Вера",
        "Монстера в углу",
        "Лимонное дерево",
        "Орхидея на столе",
        "Папоротник в ванной",
        "Бонсай на полке",
        "Сансевиерия в прихожей",
        "Плющ на балконе",
        "Фикус Бенджамина",
        "Каланхоэ на кухне",
        "Драцена в гостиной",
        "Пеперомия на подоконнике",

    )
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        // 1. Кнопка должна лежать ЗДЕСЬ — внутри круглых скобок Scaffold, в своем личном слоте!
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* TODO: Add new plant */ }
            ) {
                Text("+")
            }
        }
        // 2. А вот здесь круглые скобки закрываются, и начинаются фигурные ТОЛЬКО для контента
    ) { innerPadding ->
        //info row


        Column(
            modifier = Modifier
                .padding(innerPadding)
                //.padding(16.dp)
                .fillMaxSize()
        ) {
            Header()
            //Spacer(modifier = Modifier.height(16.dp))
            PlantList(plants = myPlants, onPlantClick = { /* TODO: Handle plant click */ })
        }
    } // Конец Scaffold
}


@Composable
fun Header(){

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 18.dp, // Высота тени (чем больше dp, тем мягче и заметнее тень)
        color = MaterialTheme.colorScheme.surfaceContainer, // Цвет фона, можно использовать цвет из темы
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp),) {
            Text(
                text = "GSmart Windowsill",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp) // Правильный отступ без багов
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                StatusSensorCard(
                    label = "Темп.",
                    value = "24°C",
                    icon = "🌡️",
                    modifier = Modifier.weight(1f)
                )
                StatusSensorCard(
                    label = "Влажн.",
                    value = "45%",
                    icon = "💧",
                    modifier = Modifier.weight(1f)
                )
                StatusSensorCard(
                    label = "Свет",
                    value = "80%",
                    icon = "☀️",
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = "",
                onValueChange = {},
                label = { Text("Search") },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun PlantList(plants: List<String>, onPlantClick: (String) -> Unit){

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        items(plants.size) { index ->
            PlantCard(name = plants[index], onClick = { onPlantClick(plants[index]) })
        }
    }
}
@Composable
fun StatusSensorCard(label: String, value: String, icon: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = icon, fontSize = 16.sp)

            Spacer(modifier = Modifier.width(8.dp))

            Column {
                Text(text = label, fontSize = 14.sp)
                Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
@Composable
fun PlantCard(name : String, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable{ onClick()}, shape = RoundedCornerShape(16.dp ), colors = CardDefaults.cardColors(
containerColor = MaterialTheme.colorScheme.surfaceVariant
    )) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ){
            Text(text = "🌱", fontSize = 32.sp)

            Spacer(modifier = Modifier.width(16.dp))

            Column{
                Text(
                text = name,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                        text = "Статус: Подключено",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)

                )
            }


        }



    }
}

@Preview(showBackground = true)
@Composable
fun PlantListScreenPreview() {
    PlantListScreen()
}