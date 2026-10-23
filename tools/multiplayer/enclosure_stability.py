"""Observe actual rendered enclosure poses on both clients across multiple world ticks."""
import json
import time
from control import rpc, snapshot, TEST_ROOT


def sample(expected, seconds=5):
    home = rpc("server", "enclosure", x=8, y=64, z=8)
    identities = set(home["ids"])
    assert home["inside"] == expected and len(identities) == expected, home
    for side in ("owner", "guest"):
        rpc(side, "resetEnclosurePoseProbe")
    start = time.monotonic()
    while time.monotonic() - start < seconds:
        for side in ("owner", "guest"):
            state = snapshot(side)
            assert state.get("connected") and not state["renderErrors"], state
        time.sleep(.2)
    result = {}
    for side in ("owner", "guest"):
        state = snapshot(side)
        poses = {identity: pose for identity, pose in state["enclosureBatPoses"].items() if identity in identities}
        assert set(poses) == identities, (side, identities, poses)
        for identity, pose in poses.items():
            assert pose["frames"] >= 30, (side, identity, "too few rendered frames", pose)
            assert pose["maxPoseDelta"] == 0, (side, identity, "hanging pose changed", pose)
            assert pose["minPartial"] == pose["maxPartial"] == 0, (side, identity, "display animation clock is not fixed", pose)
        result[side] = poses
    return result


if __name__ == "__main__":
    result = sample(3)
    (TEST_ROOT / "enclosure-stability-results.json").write_text(json.dumps(result, indent=2))
    print(json.dumps(result, indent=2), flush=True)
