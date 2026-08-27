export interface UserVO {
  id: number
  phone: string
  nickname: string
  avatar?: string
  openid?: string
}

export interface LoginResponse {
  token: string
  user: UserVO
}

export interface LedgerVO {
  id: number
  name: string
  type: 'public' | 'personal'
  icon?: string
  theme?: string
  ownerId: number
  familyId?: number
  role?: 'creator' | 'member'
  memberCount?: number
}
