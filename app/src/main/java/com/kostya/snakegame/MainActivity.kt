package com.kostya.snakegame

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private lateinit var gameView: GameView
    private lateinit var scoreTextView: TextView
    private lateinit var highScoreTextView: TextView
    private lateinit var restartButton: Button
    private lateinit var sharedPreferences: SharedPreferences
    private var highScore = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        loadHighScore()
        setGameOverListener()
        setRestartButtonListener()

        restartGame()
    }

    private fun initViews() {
        scoreTextView = findViewById(R.id.scoreTextView)
        highScoreTextView = findViewById(R.id.highScoreTextView)
        restartButton = findViewById(R.id.restartButton)
        gameView = findViewById(R.id.gameView)
    }

    private fun loadHighScore() {
        sharedPreferences = getSharedPreferences("SnakeGamePrefs", Context.MODE_PRIVATE)
        highScore = sharedPreferences.getInt("highScore", 0)
        highScoreTextView.text = getString(R.string.high_score, highScore)
    }

    private fun setGameOverListener() {
        gameView.setOnGameOverListener { score ->
            scoreTextView.text = getString(R.string.score, score)
            if (score > highScore) {
                highScore = score
                highScoreTextView.text = getString(R.string.high_score, highScore)
                saveHighScore()
            }
            restartButton.visibility = View.VISIBLE
        }
    }

    private fun setRestartButtonListener() {
        restartButton.setOnClickListener {
            restartGame()
        }
    }

    private fun restartGame() {
        gameView.resetGame()
        scoreTextView.text = getString(R.string.score, 0)
        restartButton.visibility = View.GONE
    }

    private fun saveHighScore() {
        sharedPreferences.edit().putInt("highScore", highScore).apply()
    }
}
class GameView(context: Context) : View(context) {

    private val paint = Paint()
    private var snake = Snake()
    private var food = Food()
    private var score = 0
    private var gameOverListener: ((Int) -> Unit)? = null

    init {
        spawnFood()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawSnake(canvas)
        drawFood(canvas)
    }

    private fun drawSnake(canvas: Canvas) {
        paint.color = Color.GREEN
        snake.body.forEach {
            canvas.drawRect(it.x.toFloat(), it.y.toFloat(), (it.x + 1).toFloat(), (it.y + 1).toFloat(), paint)
        }
    }

    private fun drawFood(canvas: Canvas) {
        paint.color = Color.RED
        canvas.drawRect(food.x.toFloat(), food.y.toFloat(), (food.x + 1).toFloat(), (food.y + 1).toFloat(), paint)
    }

    private fun spawnFood() {
        food.x = Random.nextInt(0, width / 100)
        food.y = Random.nextInt(0, height / 100)
    }

    fun setOnGameOverListener(listener: (Int) -> Unit) {
        gameOverListener = listener
    }

    fun resetGame() {
        snake = Snake()
        score = 0
        spawnFood()
        invalidate() // Перерисовываем игровое поле
        postDelayed({ gameLoop() }, 100) // Запускаем игровой цикл
    }

    private fun gameLoop() {
        if (!isGameOver()) {
            snake.move()
            if (snake.eat(food)) {
                score++
                spawnFood()
            }
            invalidate() // Перерисовываем игровое поле
            postDelayed({ gameLoop() }, 100) // Продолжаем игровой цикл
        } else {
            gameOverListener?.invoke(score)
        }
    }

    private fun isGameOver(): Boolean {
        return snake.isCollidingWithWall(width / 100, height / 100) || snake.isCollidingWithSelf()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Обработка касаний для управления змейкой
        return true
    }
}
data class SnakeSegment(var x: Int, var y: Int)

class Snake {
    val body = mutableListOf<SnakeSegment>()
    private var direction = Direction.RIGHT

    init {
        for (i in 0 until 5) {
            body.add(SnakeSegment(5, 5 + i)) // Начальная позиция змейки
        }
    }

    fun move() {
        val head = body.first()
        val newHead = when (direction) {
            Direction.UP -> SnakeSegment(head.x, head.y - 1)
            Direction.DOWN -> SnakeSegment(head.x, head.y + 1)
            Direction.LEFT -> SnakeSegment(head.x - 1, head.y)
            Direction.RIGHT -> SnakeSegment(head.x + 1, head.y)
        }
        body.add(0, newHead) // Добавляем новую голову
        // Удаляем последний сегмент только если змейка не ела еду
        if (!eat(Food(newHead.x, newHead.y))) {
            body.removeAt(body.size - 1) // Удаляем последний сегмент
        }
    }

    fun eat(food: Food): Boolean {
        val head = body.first()
        if (head.x == food.x && head.y == food.y) {
            body.add(SnakeSegment(head.x, head.y)) // Увеличиваем змейку
            return true
        }
        return false
    }

    fun isCollidingWithWall(width: Int, height: Int): Boolean {
        val head = body.first()
        return head.x < 0 || head.x >= width || head.y < 0 || head.y >= height
    }

    fun isCollidingWithSelf(): Boolean {
        val head = body.first()
        return body.drop(1).any { it.x == head.x && it.y == head.y }
    }

    fun setDirection(newDirection: Direction) {
        // Изменение направления змейки
        if (direction != newDirection.opposite()) {
            direction = newDirection
        }
    }
}

data class Food(var x: Int = 0, var y: Int = 0)

enum class Direction {
    UP, DOWN, LEFT, RIGHT;

    fun opposite(): Direction {
        return when (this) {
            UP -> DOWN
            DOWN -> UP
            LEFT -> RIGHT
            RIGHT -> LEFT
        }
    }
}
