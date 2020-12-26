package creature;

import java.net.URL;
import java.util.ArrayList;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import javafx.scene.canvas.GraphicsContext;
import java.lang.Math;
import javafx.scene.layout.Pane;
import javafx.application.Platform;

import org.jbox2d.dynamics.Body;

import runway.Runway;
import view.MainCanvas;

public class Creature implements Runnable {

    protected int power;

    protected int posX;

    protected int posY;

    protected int moveSpeed;

    protected int defaultMoveSpeed = 1;

    protected int figureSize;

    //protected Body body; // 矩形刚体

    protected boolean belongToMe;

    protected String name;

    protected Image image;

    protected ImageView imageView;

    protected Image cardImage; //位于卡牌区时的卡

    protected Runway runway; // 所处的跑道

    protected boolean isRunning = true;

    protected int price;

    public Creature() {
        imageView = new ImageView();
        loadImage("huluwa.png");
        
        this.figureSize = (int)(imageView.getFitWidth() / 2);
        this.power = 1; // TODO 别的方式初始化
        this.moveSpeed = defaultMoveSpeed = 1;
    }

    protected void loadImage(String imageName) {
        URL url = getClass().getClassLoader().getResource(imageName);
        System.out.println("loadImage: " + url);
        image = new Image(url.toString());
        
        imageView.setImage(image); // TODO 这里会不会导致脱节
        imageView.setFitWidth(50);
        imageView.setPreserveRatio(true); //保持比例，但fitheight由于没设置，会是0！
        cardImage = new Image(url.toString()); //TODO 改掉
    }

    public void move() {
        if (belongToMe) {
            posX = posX + moveSpeed;
        } else {
            posX = posX - moveSpeed;
        }
    }

    @Override
    public void run() {
        while (!Thread.interrupted() && isRunning) {
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    draw();
                    update();
                }
            });
            try {
                Thread.sleep(20);
            } catch (Exception e) {
                Thread.currentThread().interrupt();
            }
        }
        System.out.println("creature线程结束");
    }

    public void draw() {
        imageView.setImage(image);
        imageView.setLayoutX(posX);
        imageView.setLayoutY(posY);
    }

    public void update() {
        // 获取我方生物
        ArrayList<Creature> myCreatures = (belongToMe == true) ? runway.getMyCreatures() : runway.getYourCreatures();
        // 获取敌方生物
        ArrayList<Creature> enemyCreatures = (belongToMe == true) ? runway.getYourCreatures() : runway.getMyCreatures();

        // 跑出跑道就结束
        if (posX < runway.getPosX() || posX > runway.getPosX() + runway.getLength()) {
            // TODO 别的方法跳出
            isRunning = false;
            if (belongToMe) {
                runway.removeFromMyCreature(this);
            } else {
                runway.removeFromYourCreature(this);
            }
        }

        // 如果是我的队头，看看和敌方队头碰了没，没碰需要恢复我方速度，然后继续走
        // 碰了往后计算贴着自己的队友有哪些，计算Mpower，再算对面的Ypower
        // 如果Mpower > Ypower，Ms中的人速度变为1，Ys中的人速度变为-1
        // 如果相等，大家都变0
        //
        // 如果是我的队员，看看前面碰了没，没碰继续走，碰了看前面人的速度
        // 碰了也继续走，因为你的速度已经被队头改了，所以走就完事了。。
        // TODO 线程安全问题

        if (myCreatures.indexOf(this) == 0) {
            // 队头
            if (enemyCreatures.size() > 0 && isCollide(enemyCreatures.get(0))) {
                if (enemyCreatures.get(0).getMoveSpeed() < 0) {
                    //如果对面头是负的速度，直接不管，不用比了
                    //解决N:1由于N的速度不一致而贴合不紧
                    //导致一会N:1一会1:1，那个对面的1速度不稳的问题
                    move();
                } else {
                    ArrayList<Creature> myHeadCreatures = headLinkedCreatures(myCreatures);
                    ArrayList<Creature> enemyHeadCreatures = headLinkedCreatures(enemyCreatures);
                    int myPower = 0, enemyPower = 0; // 计算力量和

                    for (Creature c : myHeadCreatures) {
                        myPower = myPower + c.getPower();
                    }
                    for (Creature c : enemyHeadCreatures) {
                        enemyPower = enemyPower + c.getPower();
                    }

                    System.out.println("belongToME: " + belongToMe + ", myPower: " + myPower + ", enemyPower: "
                            + enemyPower + ", 连着的兄弟有 " + myHeadCreatures.size() + " 个，敌人兄弟有 "
                            + enemyHeadCreatures.size() + " 个");

                    // 力量相等，速度都归0
                    if (myPower == enemyPower) {
                        myHeadCreatures.forEach(c -> c.setMoveSpeed(0));
                        enemyHeadCreatures.forEach(c -> c.setMoveSpeed(0));
                        System.out.println("力量一样，我的当前速度 " + moveSpeed);
                    } else if (myPower > enemyPower) {
                        // 我方力量大，压迫对面
                        myHeadCreatures.forEach(c -> c.setMoveSpeed(c.getDefaultMoveSpeed()));
                        enemyHeadCreatures.forEach(c -> c.setMoveSpeed(-1 * c.getDefaultMoveSpeed()));
                        // 让强的一方贴紧点
                        for (int i = 1; i < myHeadCreatures.size(); i++) {
                            for (int k = 0; k < 2 * i; k++) {
                                myHeadCreatures.get(i).move();
                            }
                        }
                        System.out.println("我方力量大，我的当前速度 " + moveSpeed);
                    } else {
                        myHeadCreatures.forEach(c -> c.setMoveSpeed(-1 * c.getDefaultMoveSpeed()));
                        enemyHeadCreatures.forEach(c -> c.setMoveSpeed(c.getDefaultMoveSpeed()));
                        /*
                         * for (Creature c : myHeadCreatures) { c.moveSpeed = -1; } for (Creature c :
                         * enemyHeadCreatures) { c.moveSpeed = 1; }
                         */

                        System.out.println("我方力量小，我的当前速度 " + moveSpeed);
                    }
                    move();
                }

            } else {
                // 恢复速度，出现在比如闪电卡打死对面队头后局势逆转
                for (int i = 0; i < myCreatures.size(); i++) {
                    myCreatures.get(i).setMoveSpeed(defaultMoveSpeed);
                }
                move();
            }
        } else {
            // 队员
            // TODO 需要考虑前方队友被冰冻，我撞上去也得速度归0，而不是移动
            move();
        }

    }

    public ArrayList<Creature> headLinkedCreatures(ArrayList<Creature> creatures) {
        ArrayList<Creature> linkedCreatures = new ArrayList<Creature>();
        if (creatures.size() > 0) {
            linkedCreatures.add(creatures.get(0)); // 自己加进去
        }
        for (int i = 1; i < creatures.size(); i++) {
            if (creatures.get(i - 1).isCollide(creatures.get(i))) {
                linkedCreatures.add(creatures.get(i)); // 邻接
            } else {
                break; // 不邻接直接断
            }
        }
        return linkedCreatures;
    }

    public boolean isCollide(Creature other) {
        return this.posY == other.posY && Math.abs(this.posX - other.posX) <= this.figureSize + other.figureSize;
    }

    public void addToPane(Pane pane) {
        pane.getChildren().add(imageView);
    }

    public int getPosX() {
        return posX;
    }

    public int getPosY() {
        return posY;
    }

    public Image getImage() {
        return this.image;
    }

    public void setRunway(Runway runway) {
        this.runway = runway;
    }

    public int getPower() {
        return power;
    }

    public void setPower(int power) {
        this.power = power;
    }

    public void setPosX(int posX) {
        this.posX = posX;
    }

    public void setPosY(int posY) {
        this.posY = posY;
    }

    public int getMoveSpeed() {
        return moveSpeed;
    }

    public void setMoveSpeed(int moveSpeed) {
        this.moveSpeed = moveSpeed;
    }

    public int getDefaultMoveSpeed() {
        return defaultMoveSpeed;
    }

    public void setDefaultMoveSpeed(int defaultMoveSpeed) {
        this.defaultMoveSpeed = defaultMoveSpeed;
    }

    public int getFigureSize() {
        return figureSize;
    }

    public void setFigureSize(int figureSize) {
        this.figureSize = figureSize;
    }

    public boolean isBelongToMe() {
        return belongToMe;
    }

    public void setBelongToMe(boolean belongToMe) {
        this.belongToMe = belongToMe;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setImage(Image image) {
        this.image = image;
    }

    public ImageView getImageView() {
        return imageView;
    }

    public void setImageView(ImageView imageView) {
        this.imageView = imageView;
    }

    public Runway getRunway() {
        return runway;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void setRunning(boolean isRunning) {
        this.isRunning = isRunning;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public Image getCardImage() {
        return cardImage;
    }

    public void setCardImage(Image cardImage) {
        this.cardImage = cardImage;
    }
}