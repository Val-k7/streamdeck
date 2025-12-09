from fastapi import APIRouter, HTTPException

from ..utils.profile_manager import ProfileManager

router = APIRouter()
profiles = ProfileManager()


@router.get("/")
async def list_profiles():
    return {"profiles": profiles.list_profiles()}


@router.get("/{profile_id}")
async def get_profile(profile_id: str):
    data = profiles.get_profile(profile_id)
    if not data:
        raise HTTPException(status_code=404, detail="Profile not found")
    return data


@router.post("/{profile_id}")
async def save_profile(profile_id: str, payload: dict):
    try:
        profile = profiles.save_profile(profile_id, payload)
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc))
    return {"status": "saved", "profileId": profile.id, "version": profile.version}


@router.delete("/{profile_id}")
async def delete_profile(profile_id: str):
    ok = profiles.delete_profile(profile_id)
    if not ok:
        raise HTTPException(status_code=404, detail="Profile not found")
    return {"status": "deleted", "profileId": profile_id}
