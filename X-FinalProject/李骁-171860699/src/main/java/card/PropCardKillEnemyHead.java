package card;

import view.MainCanvas;
import runway.Runway;

public class PropCardKillEnemyHead extends DraggableCard {

    public PropCardKillEnemyHead() {
        loadImage(this.getClass().getSimpleName() + ".png");
    }

    @Override
    protected void cardAction() {
        //TODO 给对方发包
        runway.killEnemyHead();
    }
}